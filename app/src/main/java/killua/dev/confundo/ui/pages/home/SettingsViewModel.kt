package killua.dev.confundo.ui.pages.home

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import killua.dev.confundo.data.AppSettings
import killua.dev.confundo.data.SettingsRepository
import killua.dev.confundo.ui.viewmodel.BaseViewModel
import killua.dev.confundo.ui.viewmodel.UIIntent
import killua.dev.confundo.ui.viewmodel.UIState
import killua.dev.confundo.work.RefreshWorker
import javax.inject.Inject

data class SettingsUiState(
    val autoRefreshEnabled: Boolean = false,
    val intervalDays: Int = AppSettings.DEFAULT_INTERVAL_DAYS,
    val lastRunMillis: Long = 0L,
) : UIState

sealed interface SettingsIntent : UIIntent {
    data object Load : SettingsIntent
    data class SetAutoRefresh(val enabled: Boolean) : SettingsIntent
    data class SetInterval(val days: Int) : SettingsIntent
    data object RunNow : SettingsIntent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @param:ApplicationContext private val context: Context,
) : BaseViewModel<SettingsIntent, SettingsUiState, Nothing>(SettingsUiState()) {

    private var observing = false

    override suspend fun onEvent(state: SettingsUiState, intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.Load -> observe()
            is SettingsIntent.SetAutoRefresh -> setAutoRefresh(intent.enabled)
            is SettingsIntent.SetInterval -> setInterval(intent.days)
            SettingsIntent.RunNow -> RefreshWorker.runNow(context)
        }
    }

    override suspend fun onEffect(effect: Nothing) {}

    private fun observe() {
        if (observing) return
        observing = true
        launchOnIO {
            settingsRepository.settings.collect { s ->
                emitState(
                    uiState.value.copy(
                        autoRefreshEnabled = s.autoRefreshEnabled,
                        intervalDays = s.intervalDays,
                        lastRunMillis = s.lastRunMillis,
                    )
                )
            }
        }
    }

    private suspend fun setAutoRefresh(enabled: Boolean) {
        settingsRepository.setAutoRefreshEnabled(enabled)
        if (enabled) {
            RefreshWorker.schedule(context, uiState.value.intervalDays)
        } else {
            RefreshWorker.cancel(context)
        }
    }

    private suspend fun setInterval(days: Int) {
        val clamped = days.coerceIn(AppSettings.MIN_INTERVAL_DAYS, AppSettings.MAX_INTERVAL_DAYS)
        settingsRepository.setIntervalDays(clamped)
        // 间隔变化时若已启用，重新安排周期任务。
        if (uiState.value.autoRefreshEnabled) {
            RefreshWorker.schedule(context, clamped)
        }
    }
}
