package killua.dev.confundo.ui.pages.home

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import killua.dev.confundo.data.ConfigRepository
import killua.dev.confundo.ui.viewmodel.BaseViewModel
import killua.dev.confundo.ui.viewmodel.SnackbarUIEffect
import killua.dev.confundo.ui.viewmodel.UIIntent
import killua.dev.confundo.ui.viewmodel.UIState
import killua.dev.confundo.utils.AppIconCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class CopySourceUiState(
    val phase: HomePhase = HomePhase.Loading,
    val targetPkg: String = "",
    val sources: List<AppListItem> = emptyList(),
    val icons: Map<String, ImageBitmap?> = emptyMap(),
    val searchQuery: String = "",
) : UIState {
    val visibleSources: List<AppListItem>
        get() {
            val query = searchQuery.trim()
            if (query.isEmpty()) return sources
            return sources.filter {
                it.appName.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
        }
}

sealed interface CopySourceIntent : UIIntent {
    data class Load(val targetPkg: String) : CopySourceIntent
    data class SetSearchQuery(val query: String) : CopySourceIntent
    data class Apply(val sourcePkg: String) : CopySourceIntent
}

@HiltViewModel
class CopySourceViewModel @Inject constructor(
    private val iconCache: AppIconCache,
    private val repository: ConfigRepository,
    @param:ApplicationContext private val context: Context,
) : BaseViewModel<CopySourceIntent, CopySourceUiState, SnackbarUIEffect>(CopySourceUiState()) {

    override suspend fun onEvent(state: CopySourceUiState, intent: CopySourceIntent) {
        when (intent) {
            is CopySourceIntent.Load -> load(intent.targetPkg)
            is CopySourceIntent.SetSearchQuery -> updateState { it.copy(searchQuery = intent.query) }
            is CopySourceIntent.Apply ->
                repository.copyFieldsFrom(uiState.value.targetPkg, intent.sourcePkg)
        }
    }

    private suspend fun load(targetPkg: String) {
        if (uiState.value.targetPkg == targetPkg && uiState.value.sources.isNotEmpty()) return
        emitState(uiState.value.copy(phase = HomePhase.Loading, targetPkg = targetPkg))

        val sources = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            repository.packagesWithFieldData(targetPkg).map { pkg ->
                AppListItem(
                    packageName = pkg,
                    appName = runCatching {
                        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                    }.getOrDefault(pkg),
                    isSystemApp = false,
                    isSpoofingEnabled = false,
                )
            }.sortedBy { it.appName.lowercase() }
        }

        emitState(uiState.value.copy(phase = HomePhase.Ready, sources = sources))

        launchOnIO {
            val iconMap = sources.associate { app ->
                app.packageName to runCatching { iconCache.getIcon(app.packageName) }.getOrNull()
            }
            updateState { it.copy(icons = iconMap) }
        }
    }
}
