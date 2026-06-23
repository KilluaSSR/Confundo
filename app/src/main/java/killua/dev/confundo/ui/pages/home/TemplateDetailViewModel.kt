package killua.dev.confundo.ui.pages.home

import dagger.hilt.android.lifecycle.HiltViewModel
import killua.dev.confundo.data.ConfigRepository
import killua.dev.confundo.ui.viewmodel.BaseViewModel
import killua.dev.confundo.ui.viewmodel.UIIntent
import killua.dev.confundo.ui.viewmodel.UIState
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class TemplateDetailUiState(
    val phase: DetailPhase = DetailPhase.Loading,
    val templateId: String = "",
    val name: String = "",
    val fields: Map<String, String> = emptyMap(),
    val isNew: Boolean = false,
) : UIState

sealed interface TemplateDetailIntent : UIIntent {
    data class Load(val templateId: String) : TemplateDetailIntent
    data class SetName(val name: String) : TemplateDetailIntent
    data class UpdateField(val key: String, val value: String) : TemplateDetailIntent
    data object RandomFill : TemplateDetailIntent
}

@HiltViewModel
class TemplateDetailViewModel @Inject constructor(
    private val repository: ConfigRepository,
) : BaseViewModel<TemplateDetailIntent, TemplateDetailUiState, Nothing>(TemplateDetailUiState()) {

    companion object {
        const val NEW_ID = "new"
    }

    private val createMutex = Mutex()
    private var observeJob: Job? = null
    private var loaded = false

    override suspend fun onEvent(state: TemplateDetailUiState, intent: TemplateDetailIntent) {
        when (intent) {
            is TemplateDetailIntent.Load -> load(intent.templateId)
            is TemplateDetailIntent.SetName -> setName(intent.name)
            is TemplateDetailIntent.UpdateField -> updateField(intent.key, intent.value)
            TemplateDetailIntent.RandomFill -> randomFill()
        }
    }

    override suspend fun onEffect(effect: Nothing) {}

    private fun load(templateId: String) {
        if (loaded) return
        loaded = true
        if (templateId == NEW_ID) {
            emitState(
                TemplateDetailUiState(
                    phase = DetailPhase.Ready,
                    templateId = NEW_ID,
                    name = "",
                    fields = emptyMap(),
                    isNew = true,
                )
            )
        } else {
            observe(templateId)
        }
    }

    private fun observe(id: String) {
        observeJob?.cancel()
        observeJob = launchOnIO {
            repository.templateDetailFlow(id).collect { detail ->
                emitState(
                    uiState.value.copy(
                        phase = DetailPhase.Ready,
                        templateId = id,
                        name = detail.name,
                        fields = detail.fields,
                        isNew = false,
                    )
                )
            }
        }
    }

    private suspend fun ensureCreated(initialName: String): String = createMutex.withLock {
        val current = uiState.value
        if (!current.isNew && current.templateId.isNotEmpty()) return@withLock current.templateId
        val id = repository.createTemplate(initialName)
        emitState(current.copy(templateId = id, isNew = false))
        observe(id)
        id
    }

    private suspend fun setName(name: String) {
        val id = ensureCreated(name)
        repository.setTemplateName(id, name)
    }

    private suspend fun updateField(key: String, value: String) {
        val id = ensureCreated(uiState.value.name)
        repository.updateTemplateField(id, key, value)
    }

    private suspend fun randomFill() {
        val id = ensureCreated(uiState.value.name.ifEmpty { "新模版" })
        repository.randomFillTemplate(id)
    }
}
