package killua.dev.confundo.ui.pages.home

import android.content.Context
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import killua.dev.confundo.R
import killua.dev.confundo.data.AppSettings
import killua.dev.confundo.data.BackupRepository
import killua.dev.confundo.data.ConfigRepository
import killua.dev.confundo.data.SettingsRepository
import killua.dev.confundo.ui.viewmodel.BaseViewModel
import killua.dev.confundo.ui.viewmodel.SnackbarUIEffect
import killua.dev.confundo.ui.viewmodel.UIIntent
import killua.dev.confundo.ui.viewmodel.UIState
import killua.dev.confundo.work.RefreshWorker
import javax.inject.Inject

data class SettingsUiState(
    val autoRefreshEnabled: Boolean = false,
    val intervalDays: Int = AppSettings.DEFAULT_INTERVAL_DAYS,
    val lastRunMillis: Long = 0L,
    val darkMode: Int = AppSettings.DARK_MODE_SYSTEM,
    val dynamicColor: Boolean = true,
    val randomizeActivationTime: Boolean = false,
    val randomizeBootTime: Boolean = false,
) : UIState

sealed interface SettingsIntent : UIIntent {
    data object Load : SettingsIntent
    data class SetAutoRefresh(val enabled: Boolean) : SettingsIntent
    data class SetInterval(val days: Int) : SettingsIntent
    data object RunNow : SettingsIntent
    data class SetDarkMode(val mode: Int) : SettingsIntent
    data class SetDynamicColor(val enabled: Boolean) : SettingsIntent
    data class SetRandomizeActivationTime(val enabled: Boolean) : SettingsIntent
    data class SetRandomizeBootTime(val enabled: Boolean) : SettingsIntent
    data object ClearAllActivationTime : SettingsIntent
    data object ClearAllBootTime : SettingsIntent
    data class ExportBackup(val uri: Uri) : SettingsIntent
    data class ImportBackup(val uri: Uri) : SettingsIntent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val configRepository: ConfigRepository,
    private val backupRepository: BackupRepository,
    @param:ApplicationContext private val context: Context,
) : BaseViewModel<SettingsIntent, SettingsUiState, SnackbarUIEffect>(SettingsUiState()) {

    private var observing = false

    override suspend fun onEvent(state: SettingsUiState, intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.Load -> observe()
            is SettingsIntent.SetAutoRefresh -> setAutoRefresh(intent.enabled)
            is SettingsIntent.SetInterval -> setInterval(intent.days)
            SettingsIntent.RunNow -> runNow()
            is SettingsIntent.SetDarkMode -> settingsRepository.setDarkMode(intent.mode)
            is SettingsIntent.SetDynamicColor -> settingsRepository.setDynamicColor(intent.enabled)
            is SettingsIntent.SetRandomizeActivationTime ->
                settingsRepository.setRandomizeActivationTime(intent.enabled)
            is SettingsIntent.SetRandomizeBootTime ->
                settingsRepository.setRandomizeBootTime(intent.enabled)
            SettingsIntent.ClearAllActivationTime -> clearAll(
                FieldKeys.ACTIVATION_TIME,
                R.string.settings_time_cleared_activation,
            )
            SettingsIntent.ClearAllBootTime -> clearAll(
                FieldKeys.BOOT_TIME,
                R.string.settings_time_cleared_boot,
            )
            is SettingsIntent.ExportBackup -> exportBackup(intent.uri)
            is SettingsIntent.ImportBackup -> importBackup(intent.uri)
        }
    }

    private suspend fun exportBackup(uri: Uri) {
        runCatching { backupRepository.exportTo(uri) }
            .onSuccess {
                emitEffect(
                    SnackbarUIEffect.ShowSnackbar(context.getString(R.string.settings_backup_export_done))
                )
            }
            .onFailure { e ->
                emitEffect(
                    SnackbarUIEffect.ShowSnackbar(
                        context.getString(
                            R.string.settings_backup_export_failed,
                            e.localizedMessage ?: e.javaClass.simpleName,
                        )
                    )
                )
            }
    }

    private suspend fun importBackup(uri: Uri) {
        runCatching { backupRepository.restoreFrom(uri) }
            .onSuccess { summary ->
                emitEffect(
                    SnackbarUIEffect.ShowSnackbar(
                        context.getString(
                            R.string.settings_backup_import_done,
                            summary.restoredApps,
                            summary.skippedApps,
                            summary.restoredTemplates,
                        )
                    )
                )
            }
            .onFailure { e ->
                emitEffect(
                    SnackbarUIEffect.ShowSnackbar(
                        context.getString(
                            R.string.settings_backup_import_failed,
                            e.localizedMessage ?: e.javaClass.simpleName,
                        )
                    )
                )
            }
    }

    private suspend fun clearAll(key: String, messageRes: Int) {
        configRepository.clearFieldForAll(key)
        emitEffect(SnackbarUIEffect.ShowSnackbar(context.getString(messageRes)))
    }

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
                        darkMode = s.darkMode,
                        dynamicColor = s.dynamicColor,
                        randomizeActivationTime = s.randomizeActivationTime,
                        randomizeBootTime = s.randomizeBootTime,
                    )
                )
            }
        }
    }

    private suspend fun runNow() {
        RefreshWorker.runNow(context)
        emitEffect(SnackbarUIEffect.ShowSnackbar(context.getString(R.string.settings_run_now_done)))
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
