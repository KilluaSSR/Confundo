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
import killua.dev.confundo.R
import killua.dev.confundo.data.ConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class TemplateDetailUiState(
    val phase: DetailPhase = DetailPhase.Loading,
    val templateId: String = "",
    val name: String = "",
    val fields: Map<String, String> = emptyMap(),
    val isNew: Boolean = false,
) : UiState

sealed interface TemplateDetailIntent : UiIntent {
    data class Load(val templateId: String) : TemplateDetailIntent
    data class SetName(val name: String) : TemplateDetailIntent
    data class UpdateField(val key: String, val value: String) : TemplateDetailIntent
    data object RandomFill : TemplateDetailIntent
    data object Back : TemplateDetailIntent
}

sealed interface TemplateDetailEffect : UiEffect

@HiltViewModel
class TemplateDetailViewModel @Inject constructor(
    private val repository: ConfigRepository,
    private val navigator: Navigator,
    private val overlay: OverlayController,
    @param:ApplicationContext private val context: Context,
) : BaseViewModel<TemplateDetailUiState, TemplateDetailIntent, TemplateDetailEffect>(TemplateDetailUiState()) {

    companion object {
        const val NEW_ID = "new"
    }

    private val createMutex = Mutex()
    private var observeJob: Job? = null
    private var loaded = false

    override suspend fun handleIntent(intent: TemplateDetailIntent) {
        when (intent) {
            is TemplateDetailIntent.Load -> load(intent.templateId)
            is TemplateDetailIntent.SetName -> setName(intent.name)
            is TemplateDetailIntent.UpdateField -> updateField(intent.key, intent.value)
            TemplateDetailIntent.RandomFill -> randomFill()
            TemplateDetailIntent.Back -> navigator.navigateUp()
        }
    }

    private fun load(templateId: String) {
        if (loaded) return
        loaded = true
        if (templateId == NEW_ID) {
            setState {
                copy(
                    phase = DetailPhase.Ready,
                    templateId = NEW_ID,
                    name = "",
                    fields = emptyMap(),
                    isNew = true,
                )
            }
        } else {
            observe(templateId)
        }
    }

    private fun observe(id: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch(Dispatchers.IO) {
            repository.templateDetailFlow(id).collect { detail ->
                setState {
                    copy(
                        phase = DetailPhase.Ready,
                        templateId = id,
                        name = detail.name,
                        fields = detail.fields,
                        isNew = false,
                    )
                }
            }
        }
    }

    private suspend fun ensureCreated(initialName: String): String = createMutex.withLock {
        val current = currentState
        if (!current.isNew && current.templateId.isNotEmpty()) return@withLock current.templateId
        val id = repository.createTemplate(initialName)
        setState { copy(templateId = id, isNew = false) }
        observe(id)
        id
    }

    private suspend fun setName(name: String) {
        val id = ensureCreated(name)
        repository.setTemplateName(id, name)
    }

    private suspend fun updateField(key: String, value: String) {
        val id = ensureCreated(currentState.name)
        repository.updateTemplateField(id, key, value)
    }

    private suspend fun randomFill() {
        val id = ensureCreated(currentState.name.ifEmpty { "新模版" })
        repository.randomFillTemplate(id)
        overlay.snackbar(context.getString(R.string.random_fill_done))
    }
}
