package killua.dev.confundo.ui.pages.home

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import killua.dev.arsenal.core.mvvm.BaseViewModel
import killua.dev.arsenal.core.mvvm.UiEffect
import killua.dev.arsenal.core.mvvm.UiIntent
import killua.dev.arsenal.core.mvvm.UiState
import killua.dev.arsenal.core.overlay.OverlayController
import killua.dev.arsenal.navigation.Navigator
import killua.dev.arsenal.navigation.generated.GeneratedRoutes
import killua.dev.confundo.R
import killua.dev.confundo.data.ConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AppListItem(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val isSpoofingEnabled: Boolean,
)

enum class HomePhase { Loading, Ready }

/** 按伪装启用状态筛选。 */
enum class AppStatusFilter { All, Enabled, Disabled }

/** 列表排序方式。 */
enum class AppSortOrder { NameAsc, NameDesc, EnabledFirst }

data class HomeUiState(
    val phase: HomePhase = HomePhase.Loading,
    val apps: List<AppListItem> = emptyList(),
    val visibleApps: List<AppListItem> = emptyList(),
    val showSystemApps: Boolean = false,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val statusFilter: AppStatusFilter = AppStatusFilter.All,
    val sortOrder: AppSortOrder = AppSortOrder.NameAsc,
    val selectedPkgs: Set<String> = emptySet(),
) : UiState {
    val allVisibleSelected: Boolean
        get() = visibleApps.isNotEmpty() && visibleApps.all { it.packageName in selectedPkgs }
}

private data class VisibleKey(
    val apps: List<AppListItem>,
    val showSystemApps: Boolean,
    val searchQuery: String,
    val statusFilter: AppStatusFilter,
    val sortOrder: AppSortOrder,
)

sealed interface HomeIntent : UiIntent {
    data object Load : HomeIntent
    data object Refresh : HomeIntent
    data object ToggleSystemApps : HomeIntent
    data object ApplyToAll : HomeIntent
    data class ToggleSelect(val pkg: String) : HomeIntent
    data object ClearSelection : HomeIntent
    data object ToggleSelectAllVisible : HomeIntent
    data class BatchSetEnabled(val enabled: Boolean) : HomeIntent
    data class BatchSetNativeHook(val enabled: Boolean) : HomeIntent
    data class BatchSetAutoReset(val autoReset: Boolean) : HomeIntent
    data class ApplyTemplate(val templateId: String) : HomeIntent
    data class SetSearchQuery(val query: String) : HomeIntent
    data class SetStatusFilter(val filter: AppStatusFilter) : HomeIntent
    data class SetSortOrder(val order: AppSortOrder) : HomeIntent
    data class OpenAppDetail(val pkg: String) : HomeIntent
    data object OpenTemplateManage : HomeIntent
}

sealed interface HomeEffect : UiEffect

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ConfigRepository,
    private val navigator: Navigator,
    private val overlay: OverlayController,
    @param:ApplicationContext private val context: Context,
) : BaseViewModel<HomeUiState, HomeIntent, HomeEffect>(HomeUiState()) {

    init {
        observeEnabledChanges()
        observeVisibleApps()
    }

    override suspend fun handleIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Load -> load(force = false)
            HomeIntent.Refresh -> load(force = true)
            HomeIntent.ToggleSystemApps -> toggleSystemApps()
            HomeIntent.ApplyToAll -> applyToAll()
            is HomeIntent.ToggleSelect -> toggleSelect(intent.pkg)
            HomeIntent.ClearSelection -> setState { copy(selectedPkgs = emptySet()) }
            HomeIntent.ToggleSelectAllVisible -> toggleSelectAllVisible()
            is HomeIntent.BatchSetEnabled -> batchSetEnabled(intent.enabled)
            is HomeIntent.BatchSetNativeHook -> batchSetNativeHook(intent.enabled)
            is HomeIntent.BatchSetAutoReset -> batchSetAutoReset(intent.autoReset)
            is HomeIntent.ApplyTemplate -> applyTemplate(intent.templateId)
            is HomeIntent.SetSearchQuery -> setState { copy(searchQuery = intent.query) }
            is HomeIntent.SetStatusFilter -> setState { copy(statusFilter = intent.filter) }
            is HomeIntent.SetSortOrder -> setState { copy(sortOrder = intent.order) }
            is HomeIntent.OpenAppDetail -> navigator.navigate(GeneratedRoutes.appDetailPage(intent.pkg))
            HomeIntent.OpenTemplateManage -> navigator.navigate(GeneratedRoutes.TEMPLATE_MANAGE_PAGE)
        }
    }

    private fun notify(message: String) {
        overlay.snackbar(message)
    }

    private fun observeEnabledChanges() = viewModelScope.launch(Dispatchers.IO) {
        repository.enabledChanges().collect { (pkg, enabled) ->
            setState {
                copy(apps = apps.map {
                    if (it.packageName == pkg) it.copy(isSpoofingEnabled = enabled) else it
                })
            }
        }
    }

    private fun observeVisibleApps() = viewModelScope.launch(Dispatchers.IO) {
        uiState
            .map { VisibleKey(it.apps, it.showSystemApps, it.searchQuery, it.statusFilter, it.sortOrder) }
            .distinctUntilChanged()
            .collectLatest { key ->
                val visible = filterAndSort(key)
                setState { copy(visibleApps = visible) }
            }
    }

    private suspend fun load(force: Boolean) {
        val current = currentState
        if (!force && current.apps.isNotEmpty()) return
        if (force) setState { copy(isRefreshing = true) }
        else setState { copy(phase = HomePhase.Loading) }

        val apps = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val base = packages
                .filter { it.packageName != context.packageName }
                .map { appInfo ->
                    AppListItem(
                        packageName = appInfo.packageName,
                        appName = runCatching { pm.getApplicationLabel(appInfo).toString() }
                            .getOrDefault(appInfo.packageName),
                        isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
                        isSpoofingEnabled = false,
                    )
                }
            val enabled = repository.enabledStates(base.map { it.packageName })
            base.map { it.copy(isSpoofingEnabled = enabled[it.packageName] == true) }
        }

        val cur = currentState
        val initialVisible = withContext(Dispatchers.Default) {
            filterAndSort(
                VisibleKey(apps, cur.showSystemApps, cur.searchQuery, cur.statusFilter, cur.sortOrder)
            )
        }
        setState {
            copy(
                phase = HomePhase.Ready,
                apps = apps,
                visibleApps = initialVisible,
                isRefreshing = false,
            )
        }

    }

    private fun filterAndSort(key: VisibleKey): List<AppListItem> {
        val query = key.searchQuery.trim()
        return key.apps.asSequence()
            .filter { !it.isSystemApp || key.showSystemApps }
            .filter {
                when (key.statusFilter) {
                    AppStatusFilter.All -> true
                    AppStatusFilter.Enabled -> it.isSpoofingEnabled
                    AppStatusFilter.Disabled -> !it.isSpoofingEnabled
                }
            }
            .filter {
                query.isEmpty() ||
                    it.appName.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
            .sortedWith(
                when (key.sortOrder) {
                    AppSortOrder.NameAsc -> compareBy { it.appName.lowercase() }
                    AppSortOrder.NameDesc -> compareByDescending { it.appName.lowercase() }
                    AppSortOrder.EnabledFirst ->
                        compareByDescending<AppListItem> { it.isSpoofingEnabled }
                            .thenBy { it.appName.lowercase() }
                }
            )
            .toList()
    }

    private fun toggleSystemApps() {
        setState { copy(showSystemApps = !showSystemApps) }
    }

    private suspend fun applyToAll() {
        val current = currentState
        val targets = current.apps.filter { app ->
            !app.isSpoofingEnabled && (!app.isSystemApp || current.showSystemApps)
        }
        targets.forEach { repository.applyRandom(it.packageName, autoReset = false) }
        notify(context.getString(R.string.apply_all_done))
    }

    private fun toggleSelect(pkg: String) {
        setState {
            copy(selectedPkgs = if (pkg in selectedPkgs) selectedPkgs - pkg else selectedPkgs + pkg)
        }
    }

    private fun toggleSelectAllVisible() {
        val all = currentState.visibleApps.map { it.packageName }
        setState {
            copy(selectedPkgs = if (all.isNotEmpty() && all.all { it in selectedPkgs }) emptySet() else all.toSet())
        }
    }

    private suspend fun batchSetEnabled(enabled: Boolean) {
        val pkgs = currentState.selectedPkgs.toList()
        setState { copy(selectedPkgs = emptySet()) }
        pkgs.forEach { repository.setEnabled(it, enabled) }
        val res = if (enabled) R.string.feedback_enabled_done else R.string.feedback_disabled_done
        notify(context.getString(res, pkgs.size))
    }

    private suspend fun batchSetNativeHook(enabled: Boolean) {
        val pkgs = currentState.selectedPkgs.toList()
        setState { copy(selectedPkgs = emptySet()) }
        pkgs.forEach { pkg ->
            if (enabled) repository.setEnabled(pkg, true)
            repository.setAppNativeHook(pkg, enabled)
        }
        val res = if (enabled) R.string.feedback_native_on_done else R.string.feedback_native_off_done
        notify(context.getString(res, pkgs.size))
    }

    private suspend fun batchSetAutoReset(autoReset: Boolean) {
        val pkgs = currentState.selectedPkgs.toList()
        setState { copy(selectedPkgs = emptySet()) }
        pkgs.forEach { repository.setAutoReset(it, autoReset) }
        val res = if (autoReset) R.string.feedback_reset_on_done else R.string.feedback_reset_off_done
        notify(context.getString(res, pkgs.size))
    }

    private suspend fun applyTemplate(templateId: String) {
        val pkgs = currentState.selectedPkgs.toList()
        setState { copy(selectedPkgs = emptySet()) }
        pkgs.forEach { repository.applyTemplate(it, templateId) }
        notify(context.getString(R.string.feedback_apply_template_done, pkgs.size))
    }

    suspend fun loadTemplates(): List<TemplateItem> =
        repository.getTemplates().map { TemplateItem(it.id, it.name) }
}
