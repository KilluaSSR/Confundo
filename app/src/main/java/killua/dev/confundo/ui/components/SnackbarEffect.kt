package killua.dev.confundo.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import killua.dev.confundo.ui.viewmodel.SnackbarUIEffect
import kotlinx.coroutines.flow.Flow

/** 统一的 Snackbar 反馈收集器：把 ViewModel 的 [SnackbarUIEffect] 流接到页面的 [SnackbarHostState]。 */
@Composable
fun ObserveSnackbarEffects(
    effects: Flow<SnackbarUIEffect>,
    hostState: SnackbarHostState,
) {
    LaunchedEffect(effects, hostState) {
        effects.collect { effect ->
            when (effect) {
                is SnackbarUIEffect.ShowSnackbar -> {
                    val result = hostState.showSnackbar(
                        message = effect.message,
                        actionLabel = effect.actionLabel,
                        withDismissAction = effect.withDismissAction,
                        duration = effect.duration,
                    )
                    when (result) {
                        SnackbarResult.ActionPerformed -> effect.onActionPerformed?.invoke()
                        SnackbarResult.Dismissed -> effect.onDismissed?.invoke()
                    }
                }

                SnackbarUIEffect.DismissSnackbar -> hostState.currentSnackbarData?.dismiss()
            }
        }
    }
}
