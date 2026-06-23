package killua.dev.confundo.ui.viewmodel

import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface UIIntent
interface UIState
interface UIEffect

sealed interface SnackbarUIEffect : UIEffect {
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val withDismissAction: Boolean = false,
        val duration: SnackbarDuration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Indefinite,
        val onActionPerformed: (suspend () -> Unit)? = null,
        val onDismissed: (suspend () -> Unit)? = null,
    ) : SnackbarUIEffect

    data object DismissSnackbar : SnackbarUIEffect
}

abstract class BaseViewModel<I : UIIntent, S : UIState, E : UIEffect>(state: S) : ViewModel() {

    fun <T> Flow<T>.flowOnIO() = flowOn(Dispatchers.IO)
    fun <T> Flow<T>.stateInScope(initValue: T) = stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = initValue
    )

    private val _uiState = MutableStateFlow(state)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<E>(extraBufferCapacity = 8)
    val effects: SharedFlow<E> = _effects.asSharedFlow()

    suspend fun withIOContext(block: suspend CoroutineScope.() -> Unit) = withContext(
        Dispatchers.IO, block
    )

    fun emitState(state: S) {
        _uiState.value = state
    }

    suspend fun emitIntent(intent: I) = withIOContext {
        onEvent(_uiState.value, intent)
    }

    fun launchOnMain(block: suspend CoroutineScope.() -> Unit) =
        viewModelScope.launch(block = block)

    suspend fun emitEffect(effect: E) {
        _effects.emit(effect)
    }

    fun emitEffectOnIO(effect: E) = launchOnIO { emitEffect(effect) }
    fun emitIntentOnIO(intent: I) = launchOnIO { emitIntent(intent) }
    fun launchOnIO(block: suspend CoroutineScope.() -> Unit) =
        viewModelScope.launch(context = Dispatchers.IO, block = block)

    protected fun updateState(transform: (S) -> S) {
        _uiState.update(transform)
    }

    protected abstract suspend fun onEvent(state: S, intent: I)
    protected abstract suspend fun onEffect(effect: E)
}
