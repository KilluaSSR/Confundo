package killua.dev.confundo.ui.pages.home

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import killua.dev.confundo.data.ConfigRepository
import killua.dev.confundo.ui.viewmodel.BaseViewModel
import killua.dev.confundo.ui.viewmodel.UIIntent
import killua.dev.confundo.ui.viewmodel.UIState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class DetailPhase { Loading, Ready }

data class AppDetailUiState(
    val phase: DetailPhase = DetailPhase.Loading,
    val appName: String = "",
    val packageName: String = "",
    val enabled: Boolean = false,
    val autoReset: Boolean = false,
    val fields: Map<String, String> = emptyMap(),
) : UIState

sealed interface AppDetailIntent : UIIntent {
    data class Load(val pkg: String) : AppDetailIntent
    data class SetEnabled(val enabled: Boolean) : AppDetailIntent
    data class SetAutoReset(val autoReset: Boolean) : AppDetailIntent
    data class UpdateField(val key: String, val value: String) : AppDetailIntent
    data object RandomFill : AppDetailIntent
}

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val repository: ConfigRepository,
    @param:ApplicationContext private val context: Context,
) : BaseViewModel<AppDetailIntent, AppDetailUiState, Nothing>(AppDetailUiState()) {

    private var observing = false

    override suspend fun onEvent(state: AppDetailUiState, intent: AppDetailIntent) {
        when (intent) {
            is AppDetailIntent.Load -> load(intent.pkg)
            is AppDetailIntent.SetEnabled -> repository.setEnabled(uiState.value.packageName, intent.enabled)
            is AppDetailIntent.SetAutoReset -> repository.setAutoReset(uiState.value.packageName, intent.autoReset)
            is AppDetailIntent.UpdateField -> repository.updateField(uiState.value.packageName, intent.key, intent.value)
            AppDetailIntent.RandomFill -> repository.randomFill(uiState.value.packageName)
        }
    }

    override suspend fun onEffect(effect: Nothing) {}

    private suspend fun load(pkg: String) {
        if (observing) return
        observing = true

        val appName = withContext(Dispatchers.IO) {
            runCatching {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            }.getOrDefault(pkg)
        }
        emitState(uiState.value.copy(phase = DetailPhase.Loading, packageName = pkg, appName = appName))

        launchOnIO {
            repository.appConfigFlow(pkg).collect { cfg ->
                emitState(
                    uiState.value.copy(
                        phase = DetailPhase.Ready,
                        packageName = pkg,
                        appName = appName,
                        enabled = cfg.enabled,
                        autoReset = cfg.autoReset,
                        fields = cfg.fields,
                    )
                )
            }
        }
    }
}
