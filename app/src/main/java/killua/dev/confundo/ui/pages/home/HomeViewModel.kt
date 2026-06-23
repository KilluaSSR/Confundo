package killua.dev.confundo.ui.pages.home

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import killua.dev.confundo.data.ConfigRepository
import killua.dev.confundo.ui.viewmodel.BaseViewModel
import killua.dev.confundo.ui.viewmodel.UIIntent
import killua.dev.confundo.ui.viewmodel.UIState
import killua.dev.confundo.utils.AppIconCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AppListItem(
    val packageName: String,
    val appName: String,
    val iconBitmap: ImageBitmap?,
    val isSystemApp: Boolean,
    val isSpoofingEnabled: Boolean,
)

enum class HomePhase { Loading, Ready }

data class HomeUiState(
    val phase: HomePhase = HomePhase.Loading,
    val apps: List<AppListItem> = emptyList(),
    val showSystemApps: Boolean = false,
    val isRefreshing: Boolean = false,
) : UIState

sealed interface HomeIntent : UIIntent {
    data object Load : HomeIntent
    data object Refresh : HomeIntent
    data object ToggleSystemApps : HomeIntent
    data object ApplyToAll : HomeIntent
    data class BatchSetEnabled(val pkgs: List<String>, val enabled: Boolean) : HomeIntent
    data class BatchSetAutoReset(val pkgs: List<String>, val autoReset: Boolean) : HomeIntent
    data class ApplyTemplate(val pkgs: List<String>, val templateId: String) : HomeIntent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val iconCache: AppIconCache,
    private val repository: ConfigRepository,
    @param:ApplicationContext private val context: Context,
) : BaseViewModel<HomeIntent, HomeUiState, Nothing>(HomeUiState()) {

    init {
        observeEnabledChanges()
    }

    override suspend fun onEvent(state: HomeUiState, intent: HomeIntent) {
        when (intent) {
            HomeIntent.Load -> load(force = false)
            HomeIntent.Refresh -> load(force = true)
            HomeIntent.ToggleSystemApps -> toggleSystemApps()
            HomeIntent.ApplyToAll -> applyToAll()
            is HomeIntent.BatchSetEnabled -> batchSetEnabled(intent.pkgs, intent.enabled)
            is HomeIntent.BatchSetAutoReset -> batchSetAutoReset(intent.pkgs, intent.autoReset)
            is HomeIntent.ApplyTemplate -> applyTemplate(intent.pkgs, intent.templateId)
        }
    }

    override suspend fun onEffect(effect: Nothing) {}

    private fun observeEnabledChanges() = launchOnIO {
        repository.enabledChanges().collect { (pkg, enabled) ->
            updateState { s ->
                s.copy(apps = s.apps.map {
                    if (it.packageName == pkg) it.copy(isSpoofingEnabled = enabled) else it
                })
            }
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
                        iconBitmap = null,
                        isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
                        isSpoofingEnabled = false,
                    )
                }
                .sortedBy { it.appName.lowercase() }
            val enabled = repository.enabledStates(base.map { it.packageName })
            base.map { it.copy(isSpoofingEnabled = enabled[it.packageName] == true) }
        }

        emitState(uiState.value.copy(phase = HomePhase.Ready, apps = apps, isRefreshing = false))

        launchOnIO {
            val withIcons = apps.map { app ->
                app.copy(iconBitmap = runCatching { iconCache.getIcon(app.packageName) }.getOrNull())
            }
            updateState { s ->
                val iconMap = withIcons.associate { it.packageName to it.iconBitmap }
                s.copy(apps = s.apps.map { it.copy(iconBitmap = iconMap[it.packageName]) })
            }
        }
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
    }

    private suspend fun batchSetEnabled(pkgs: List<String>, enabled: Boolean) {
        pkgs.forEach { repository.setEnabled(it, enabled) }
    }

    private suspend fun batchSetAutoReset(pkgs: List<String>, autoReset: Boolean) {
        pkgs.forEach { repository.setAutoReset(it, autoReset) }
    }

    private suspend fun applyTemplate(pkgs: List<String>, templateId: String) {
        pkgs.forEach { repository.applyTemplate(it, templateId) }
    }

    suspend fun loadTemplates(): List<TemplateItem> =
        repository.getTemplates().map { TemplateItem(it.id, it.name) }
}
