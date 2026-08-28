package killua.dev.confundo.ui.pages.home

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import killua.dev.confundo.R
import killua.dev.confundo.data.ConfigRepository
import killua.dev.confundo.ui.viewmodel.BaseViewModel
import killua.dev.confundo.ui.viewmodel.SnackbarUIEffect
import killua.dev.confundo.ui.viewmodel.UIIntent
import killua.dev.confundo.ui.viewmodel.UIState
import killua.dev.confundo.utils.AppIconCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    /** 图标与列表解耦：图标异步加载只更新此 map，不会触发列表重新筛选/排序。 */
    val icons: Map<String, ImageBitmap?> = emptyMap(),
    /** 已按「系统应用/状态筛选/搜索/排序」计算好的可见列表（在非主线程算好后回填）。 */
    val visibleApps: List<AppListItem> = emptyList(),
    val showSystemApps: Boolean = false,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val statusFilter: AppStatusFilter = AppStatusFilter.All,
    val sortOrder: AppSortOrder = AppSortOrder.NameAsc,
) : UIState

/** 影响可见列表的输入集合；图标不在其中，避免图标加载触发重新排序。 */
private data class VisibleKey(
    val apps: List<AppListItem>,
    val showSystemApps: Boolean,
    val searchQuery: String,
    val statusFilter: AppStatusFilter,
    val sortOrder: AppSortOrder,
)

sealed interface HomeIntent : UIIntent {
    data object Load : HomeIntent
    data object Refresh : HomeIntent
    data object ToggleSystemApps : HomeIntent
    data object ApplyToAll : HomeIntent
    data class BatchSetEnabled(val pkgs: List<String>, val enabled: Boolean) : HomeIntent
    data class BatchSetNativeHook(val pkgs: List<String>, val enabled: Boolean) : HomeIntent
    data class BatchSetAutoReset(val pkgs: List<String>, val autoReset: Boolean) : HomeIntent
    data class ApplyTemplate(val pkgs: List<String>, val templateId: String) : HomeIntent
    data class SetSearchQuery(val query: String) : HomeIntent
    data class SetStatusFilter(val filter: AppStatusFilter) : HomeIntent
    data class SetSortOrder(val order: AppSortOrder) : HomeIntent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val iconCache: AppIconCache,
    private val repository: ConfigRepository,
    @param:ApplicationContext private val context: Context,
) : BaseViewModel<HomeIntent, HomeUiState, SnackbarUIEffect>(HomeUiState()) {

    init {
        observeEnabledChanges()
        observeVisibleApps()
    }

    override suspend fun onEvent(state: HomeUiState, intent: HomeIntent) {
        when (intent) {
            HomeIntent.Load -> load(force = false)
            HomeIntent.Refresh -> load(force = true)
            HomeIntent.ToggleSystemApps -> toggleSystemApps()
            HomeIntent.ApplyToAll -> applyToAll()
            is HomeIntent.BatchSetEnabled -> batchSetEnabled(intent.pkgs, intent.enabled)
            is HomeIntent.BatchSetNativeHook -> batchSetNativeHook(intent.pkgs, intent.enabled)
            is HomeIntent.BatchSetAutoReset -> batchSetAutoReset(intent.pkgs, intent.autoReset)
            is HomeIntent.ApplyTemplate -> applyTemplate(intent.pkgs, intent.templateId)
            is HomeIntent.SetSearchQuery -> updateState { it.copy(searchQuery = intent.query) }
            is HomeIntent.SetStatusFilter -> updateState { it.copy(statusFilter = intent.filter) }
            is HomeIntent.SetSortOrder -> updateState { it.copy(sortOrder = intent.order) }
        }
    }

    private fun notify(message: String) {
        emitEffectOnIO(SnackbarUIEffect.ShowSnackbar(message))
    }

    private fun observeEnabledChanges() = launchOnIO {
        repository.enabledChanges().collect { (pkg, enabled) ->
            updateState { s ->
                s.copy(apps = s.apps.map {
                    if (it.packageName == pkg) it.copy(isSpoofingEnabled = enabled) else it
                })
            }
        }
    }

    /**
     * 在非主线程（IO）上响应式地重算可见列表：仅当 [VisibleKey] 变化时才重算，
     * 因此图标异步加载（只改 icons map）不会触发重新排序，切页也不会在主线程重复排序。
     */
    private fun observeVisibleApps() = launchOnIO {
        uiState
            .map { VisibleKey(it.apps, it.showSystemApps, it.searchQuery, it.statusFilter, it.sortOrder) }
            .distinctUntilChanged()
            .collectLatest { key ->
                val visible = filterAndSort(key)
                updateState { it.copy(visibleApps = visible) }
            }
    }

    private suspend fun load(force: Boolean) {
        val current = uiState.value
        if (!force && current.apps.isNotEmpty()) return
        if (force) emitState(current.copy(isRefreshing = true))
        else emitState(current.copy(phase = HomePhase.Loading))

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

        // 首帧即算好 visibleApps（当前仍在 IO 线程），避免出现「Ready 但列表暂空」的闪烁。
        val cur = uiState.value
        val initialVisible = withContext(Dispatchers.Default) {
            filterAndSort(
                VisibleKey(apps, cur.showSystemApps, cur.searchQuery, cur.statusFilter, cur.sortOrder)
            )
        }
        emitState(
            uiState.value.copy(
                phase = HomePhase.Ready,
                apps = apps,
                visibleApps = initialVisible,
                isRefreshing = false,
            )
        )

        launchOnIO {
            val iconMap = apps.associate { app ->
                app.packageName to runCatching { iconCache.getIcon(app.packageName) }.getOrNull()
            }
            // 仅更新图标 map；apps 不变，因此不会触发 visibleApps 的重新排序。
            updateState { it.copy(icons = iconMap) }
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
        updateState { it.copy(showSystemApps = !it.showSystemApps) }
    }

    private suspend fun applyToAll() {
        val current = uiState.value
        val targets = current.apps.filter { app ->
            !app.isSpoofingEnabled && (!app.isSystemApp || current.showSystemApps)
        }
        targets.forEach { repository.applyRandom(it.packageName, autoReset = false) }
        notify(context.getString(R.string.apply_all_done))
    }

    private suspend fun batchSetEnabled(pkgs: List<String>, enabled: Boolean) {
        pkgs.forEach { repository.setEnabled(it, enabled) }
        val res = if (enabled) R.string.feedback_enabled_done else R.string.feedback_disabled_done
        notify(context.getString(res, pkgs.size))
    }

    private suspend fun batchSetNativeHook(pkgs: List<String>, enabled: Boolean) {
        pkgs.forEach { pkg ->
            // 开启 Native Hook 需要该 App 的 Java Hook 同时开启，否则不生效。
            if (enabled) repository.setEnabled(pkg, true)
            repository.setAppNativeHook(pkg, enabled)
        }
        val res = if (enabled) R.string.feedback_native_on_done else R.string.feedback_native_off_done
        notify(context.getString(res, pkgs.size))
    }

    private suspend fun batchSetAutoReset(pkgs: List<String>, autoReset: Boolean) {
        pkgs.forEach { repository.setAutoReset(it, autoReset) }
        val res = if (autoReset) R.string.feedback_reset_on_done else R.string.feedback_reset_off_done
        notify(context.getString(res, pkgs.size))
    }

    private suspend fun applyTemplate(pkgs: List<String>, templateId: String) {
        pkgs.forEach { repository.applyTemplate(it, templateId) }
        notify(context.getString(R.string.feedback_apply_template_done, pkgs.size))
    }

    suspend fun loadTemplates(): List<TemplateItem> =
        repository.getTemplates().map { TemplateItem(it.id, it.name) }
}
