package killua.dev.confundo.ui.pages.home

import dagger.hilt.android.lifecycle.HiltViewModel
import killua.dev.confundo.data.ConfigRepository
import killua.dev.confundo.ui.viewmodel.BaseViewModel
import killua.dev.confundo.ui.viewmodel.UIIntent
import killua.dev.confundo.ui.viewmodel.UIState
import javax.inject.Inject

data class TemplateItem(
    val id: String,
    val name: String,
)

enum class TemplateManagePhase { Loading, Ready }

data class TemplateManageUiState(
    val phase: TemplateManagePhase = TemplateManagePhase.Loading,
    val templates: List<TemplateItem> = emptyList(),
) : UIState

sealed interface TemplateManageIntent : UIIntent {
    data object Load : TemplateManageIntent
    data class DeleteSelected(val ids: List<String>) : TemplateManageIntent
}

@HiltViewModel
class TemplateManageViewModel @Inject constructor(
    private val repository: ConfigRepository,
) : BaseViewModel<TemplateManageIntent, TemplateManageUiState, Nothing>(TemplateManageUiState()) {

    private var observing = false

    override suspend fun onEvent(state: TemplateManageUiState, intent: TemplateManageIntent) {
        when (intent) {
            TemplateManageIntent.Load -> observe()
            is TemplateManageIntent.DeleteSelected -> repository.deleteTemplates(intent.ids)
        }
    }

    override suspend fun onEffect(effect: Nothing) {}

    private fun observe() {
        if (observing) return
        observing = true
        launchOnIO {
            repository.templatesFlow().collect { templates ->
                emitState(
                    uiState.value.copy(
                        phase = TemplateManagePhase.Ready,
                        templates = templates.map { TemplateItem(it.id, it.name) },
                    )
                )
            }
        }
    }
}
