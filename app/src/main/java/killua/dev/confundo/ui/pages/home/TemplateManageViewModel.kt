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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TemplateItem(
    val id: String,
    val name: String,
)

enum class TemplateManagePhase { Loading, Ready }

data class TemplateManageUiState(
    val phase: TemplateManagePhase = TemplateManagePhase.Loading,
    val templates: List<TemplateItem> = emptyList(),
) : UiState

sealed interface TemplateManageIntent : UiIntent {
    data object Load : TemplateManageIntent
    data class DeleteSelected(val ids: List<String>) : TemplateManageIntent
    data object Back : TemplateManageIntent
    data class OpenTemplateDetail(val id: String) : TemplateManageIntent
}

sealed interface TemplateManageEffect : UiEffect

@HiltViewModel
class TemplateManageViewModel @Inject constructor(
    private val repository: ConfigRepository,
    private val navigator: Navigator,
    private val overlay: OverlayController,
    @param:ApplicationContext private val context: Context,
) : BaseViewModel<TemplateManageUiState, TemplateManageIntent, TemplateManageEffect>(TemplateManageUiState()) {

    private var observing = false

    override suspend fun handleIntent(intent: TemplateManageIntent) {
        when (intent) {
            TemplateManageIntent.Load -> observe()
            is TemplateManageIntent.DeleteSelected -> deleteSelected(intent.ids)
            TemplateManageIntent.Back -> navigator.navigateUp()
            is TemplateManageIntent.OpenTemplateDetail -> navigator.navigate(GeneratedRoutes.templateDetailPage(intent.id))
        }
    }

    private suspend fun deleteSelected(ids: List<String>) {
        if (ids.isEmpty()) return
        repository.deleteTemplates(ids)
        overlay.snackbar(context.getString(R.string.feedback_template_deleted, ids.size))
    }

    private fun observe() {
        if (observing) return
        observing = true
        viewModelScope.launch(Dispatchers.IO) {
            repository.templatesFlow().collect { templates ->
                setState {
                    copy(
                        phase = TemplateManagePhase.Ready,
                        templates = templates.map { TemplateItem(it.id, it.name) },
                    )
                }
            }
        }
    }
}
