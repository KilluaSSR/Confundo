package killua.dev.confundo.ui.pages.home

import android.content.Context
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
import killua.dev.confundo.data.InjectMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class DetailPhase { Loading, Ready }

data class AppDetailUiState(
    val phase: DetailPhase = DetailPhase.Loading,
    val appName: String = "",
    val packageName: String = "",
    val enabled: Boolean = false,
    val autoReset: Boolean = false,
    val nativeHookGlobalEnabled: Boolean = false,
    val nativeHookEnabled: Boolean = false,
    val injectMode: InjectMode = InjectMode.IN_APP,
    val fields: Map<String, String> = emptyMap(),
) : UiState

sealed interface AppDetailIntent : UiIntent {
    data class Load(val pkg: String) : AppDetailIntent
    data class SetEnabled(val enabled: Boolean) : AppDetailIntent
    data class SetAutoReset(val autoReset: Boolean) : AppDetailIntent
    data class SetNativeHook(val enabled: Boolean) : AppDetailIntent
    data class SetInjectMode(val mode: InjectMode) : AppDetailIntent
    data class UpdateField(val key: String, val value: String) : AppDetailIntent
    data object RandomFill : AppDetailIntent
    data object Back : AppDetailIntent
    data object OpenCopySource : AppDetailIntent
}

sealed interface AppDetailEffect : UiEffect

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val repository: ConfigRepository,
    private val navigator: Navigator,
    private val overlay: OverlayController,
    @param:ApplicationContext private val context: Context,
) : BaseViewModel<AppDetailUiState, AppDetailIntent, AppDetailEffect>(AppDetailUiState()) {

    private var observing = false

    override suspend fun handleIntent(intent: AppDetailIntent) {
        when (intent) {
            is AppDetailIntent.Load -> load(intent.pkg)
            is AppDetailIntent.SetEnabled -> repository.setEnabled(currentState.packageName, intent.enabled)
            is AppDetailIntent.SetAutoReset -> repository.setAutoReset(currentState.packageName, intent.autoReset)
            is AppDetailIntent.SetNativeHook -> repository.setAppNativeHook(currentState.packageName, intent.enabled)
            is AppDetailIntent.SetInjectMode -> repository.setInjectMode(currentState.packageName, intent.mode)
            is AppDetailIntent.UpdateField -> repository.updateField(currentState.packageName, intent.key, intent.value)
            AppDetailIntent.RandomFill -> {
                repository.randomFill(currentState.packageName)
                overlay.snackbar(context.getString(R.string.random_fill_done))
            }
            AppDetailIntent.Back -> navigator.navigateUp()
            AppDetailIntent.OpenCopySource -> navigator.navigate(GeneratedRoutes.copySourcePage(currentState.packageName))
        }
    }

    private suspend fun load(pkg: String) {
        if (observing) return
        observing = true

        val appName = withContext(Dispatchers.IO) {
            runCatching {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            }.getOrDefault(pkg)
        }
        setState { copy(phase = DetailPhase.Loading, packageName = pkg, appName = appName) }

        viewModelScope.launch(Dispatchers.IO) {
            repository.appConfigFlow(pkg).collect { cfg ->
                setState {
                    copy(
                        phase = DetailPhase.Ready,
                        packageName = pkg,
                        appName = appName,
                        enabled = cfg.enabled,
                        autoReset = cfg.autoReset,
                        nativeHookGlobalEnabled = repository.isNativeHookEnabled(),
                        nativeHookEnabled = cfg.nativeHookEnabled,
                        injectMode = cfg.injectMode,
                        fields = cfg.fields,
                    )
                }
            }
        }
    }
}
