package killua.dev.confundo.ui.pages.home

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import killua.dev.arsenal.core.mvvm.BaseViewModel
import killua.dev.arsenal.core.mvvm.UiEffect
import killua.dev.arsenal.core.mvvm.UiIntent
import killua.dev.arsenal.core.mvvm.UiState
import killua.dev.arsenal.navigation.Navigator
import killua.dev.confundo.data.ConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class CopySourceUiState(
    val phase: HomePhase = HomePhase.Loading,
    val targetPkg: String = "",
    val sources: List<AppListItem> = emptyList(),
    val searchQuery: String = "",
) : UiState {
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

sealed interface CopySourceIntent : UiIntent {
    data class Load(val targetPkg: String) : CopySourceIntent
    data class SetSearchQuery(val query: String) : CopySourceIntent
    data class Apply(val sourcePkg: String) : CopySourceIntent
    data object Back : CopySourceIntent
}

sealed interface CopySourceEffect : UiEffect

@HiltViewModel
class CopySourceViewModel @Inject constructor(
    private val repository: ConfigRepository,
    private val navigator: Navigator,
    @param:ApplicationContext private val context: Context,
) : BaseViewModel<CopySourceUiState, CopySourceIntent, CopySourceEffect>(CopySourceUiState()) {

    override suspend fun handleIntent(intent: CopySourceIntent) {
        when (intent) {
            is CopySourceIntent.Load -> load(intent.targetPkg)
            is CopySourceIntent.SetSearchQuery -> setState { copy(searchQuery = intent.query) }
            is CopySourceIntent.Apply -> {
                repository.copyFieldsFrom(currentState.targetPkg, intent.sourcePkg)
                navigator.navigateUp()
            }
            CopySourceIntent.Back -> navigator.navigateUp()
        }
    }

    private suspend fun load(targetPkg: String) {
        if (currentState.targetPkg == targetPkg && currentState.sources.isNotEmpty()) return
        setState { copy(phase = HomePhase.Loading, targetPkg = targetPkg) }

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

        setState { copy(phase = HomePhase.Ready, sources = sources) }
    }
}
