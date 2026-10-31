package killua.dev.confundo.ui.pages.settings

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import killua.dev.arsenal.automation.TaskScheduler
import killua.dev.arsenal.data.settings.SettingsStore
import killua.dev.arsenal.data.settings.asGate
import killua.dev.arsenal.data.settings.di.SettingsEntryPoint
import killua.dev.arsenal.material.ArsenalTokens
import killua.dev.arsenal.material.components.loadingindicator.ArsenalLoadingIndicator
import killua.dev.arsenal.material.overlay.LocalOverlayController
import killua.dev.arsenal.material.templates.scaffolds.plus
import killua.dev.arsenal.material.templates.settings.ArsenalSettingsScreen
import killua.dev.arsenal.material.templates.settings.ItemPosition
import killua.dev.arsenal.material.templates.settings.SettingsAccent
import killua.dev.arsenal.material.templates.settings.SettingsEntry
import killua.dev.arsenal.material.templates.settings.SettingsItemModel
import killua.dev.arsenal.material.templates.settings.SettingsItemRenderer
import killua.dev.arsenal.material.templates.settings.SettingsItemStyle
import killua.dev.arsenal.spi.annotation.SpiImpl
import killua.dev.confundo.R
import killua.dev.confundo.data.BackupRepository
import killua.dev.confundo.data.ConfigRepository
import killua.dev.confundo.data.ConfundoSettings
import killua.dev.confundo.data.DarkMode
import killua.dev.confundo.ui.components.ConfundoSurfaces
import killua.dev.confundo.ui.components.TabScaffold
import killua.dev.confundo.ui.pages.home.FieldKeys
import killua.dev.confundo.work.RefreshTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

internal const val SETTINGS_SCREEN = "settings"

private const val STYLE_REFRESH_ACTIONS = "refresh_actions"
private const val STYLE_TIME_ACTIONS = "time_actions"
private const val STYLE_BACKUP_ACTIONS = "backup_actions"

@Composable
fun SettingsTab() {
    val context = LocalContext.current
    val store = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            SettingsEntryPoint::class.java,
        ).settingsStore()
    }
    TabScaffold(title = stringResource(R.string.nav_settings)) { padding ->
        ArsenalSettingsScreen(
            screen = SETTINGS_SCREEN,
            store = store,
            contentPadding = padding + PaddingValues(
                start = ArsenalTokens.spacing.md,
                end = ArsenalTokens.spacing.md,
                bottom = ArsenalTokens.spacing.lg,
            ),
            itemContainerColor = ConfundoSurfaces.row,
        )
    }
}

// region Entries

@SpiImpl
class RefreshSwitchEntry @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsEntry {
    override val screen = SETTINGS_SCREEN
    override val group: String = context.getString(R.string.settings_auto_refresh_title)
    override val order = 0
    override fun model() = SettingsItemModel.Switch(
        definition = ConfundoSettings.autoRefreshEnabled,
        title = context.getString(R.string.settings_auto_refresh_title),
        description = context.getString(R.string.settings_auto_refresh_summary),
        icon = Icons.Rounded.Autorenew,
        accent = SettingsAccent.Primary,
    )
}

@SpiImpl
class RefreshIntervalEntry @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: SettingsStore,
) : SettingsEntry {
    override val screen = SETTINGS_SCREEN
    override val group: String = context.getString(R.string.settings_auto_refresh_title)
    override val order = 1
    override fun model() = SettingsItemModel.Slider(
        definition = ConfundoSettings.refreshIntervalDays,
        title = context.getString(R.string.settings_interval_title),
        valueRange = ConfundoSettings.MIN_INTERVAL_DAYS..ConfundoSettings.MAX_INTERVAL_DAYS,
        icon = Icons.Rounded.Schedule,
        accent = SettingsAccent.Secondary,
        valueLabel = { context.getString(R.string.settings_interval_days, it) },
        gate = ConfundoSettings.autoRefreshEnabled.asGate(store),
    )
}

@SpiImpl
class RefreshActionsEntry @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsEntry {
    override val screen = SETTINGS_SCREEN
    override val group: String = context.getString(R.string.settings_auto_refresh_title)
    override val order = 2
    override fun model() = SettingsItemModel.Custom(styleKey = STYLE_REFRESH_ACTIONS)
}

@SpiImpl
class TimeActivationEntry @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsEntry {
    override val screen = SETTINGS_SCREEN
    override val group: String = context.getString(R.string.settings_time_title)
    override val order = 10
    override fun model() = SettingsItemModel.Switch(
        definition = ConfundoSettings.randomizeActivationTime,
        title = context.getString(R.string.settings_time_randomize_activation),
        icon = Icons.Rounded.Timer,
        accent = SettingsAccent.Primary,
    )
}

@SpiImpl
class TimeBootEntry @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsEntry {
    override val screen = SETTINGS_SCREEN
    override val group: String = context.getString(R.string.settings_time_title)
    override val order = 11
    override fun model() = SettingsItemModel.Switch(
        definition = ConfundoSettings.randomizeBootTime,
        title = context.getString(R.string.settings_time_randomize_boot),
        icon = Icons.Rounded.PowerSettingsNew,
        accent = SettingsAccent.Tertiary,
    )
}

@SpiImpl
class TimeActionsEntry @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsEntry {
    override val screen = SETTINGS_SCREEN
    override val group: String = context.getString(R.string.settings_time_title)
    override val order = 12
    override fun model() = SettingsItemModel.Custom(styleKey = STYLE_TIME_ACTIONS)
}

@SpiImpl
class NativeHookEntry @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsEntry {
    override val screen = SETTINGS_SCREEN
    override val group: String = context.getString(R.string.settings_native_title)
    override val order = 20
    override fun model() = SettingsItemModel.Switch(
        definition = ConfundoSettings.nativeHookEnabled,
        title = context.getString(R.string.settings_native_hook_title),
        description = context.getString(R.string.settings_native_hook_summary),
        icon = Icons.Rounded.Bolt,
        accent = SettingsAccent.Secondary,
    )
}

@SpiImpl
class BackupActionsEntry @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsEntry {
    override val screen = SETTINGS_SCREEN
    override val group: String = context.getString(R.string.settings_backup_title)
    override val order = 30
    override fun model() = SettingsItemModel.Custom(styleKey = STYLE_BACKUP_ACTIONS)
}

@SpiImpl
class DarkModeEntry @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsEntry {
    override val screen = SETTINGS_SCREEN
    override val group: String = context.getString(R.string.settings_appearance_title)
    override val order = 40
    override fun model() = SettingsItemModel.Panel(
        definition = ConfundoSettings.darkMode,
        title = context.getString(R.string.settings_dark_mode_title),
        options = DarkMode.entries,
        optionLabel = { mode ->
            context.getString(
                when (mode) {
                    DarkMode.System -> R.string.settings_dark_mode_system
                    DarkMode.Light -> R.string.settings_dark_mode_light
                    DarkMode.Dark -> R.string.settings_dark_mode_dark
                },
            )
        },
        icon = Icons.Rounded.DarkMode,
        accent = SettingsAccent.Primary,
    )
}

@SpiImpl
class DynamicColorEntry @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsEntry {
    override val screen = SETTINGS_SCREEN
    override val group: String = context.getString(R.string.settings_appearance_title)
    override val order = 41
    override fun model() = SettingsItemModel.Switch(
        definition = ConfundoSettings.dynamicColor,
        title = context.getString(R.string.settings_dynamic_color_title),
        description = context.getString(R.string.settings_dynamic_color_summary),
        icon = Icons.Rounded.Wallpaper,
        accent = SettingsAccent.Tertiary,
    )
}

@SpiImpl
class ConfundoSettingsRenderer @Inject constructor(
    private val configRepository: ConfigRepository,
    private val backupRepository: BackupRepository,
    private val taskScheduler: TaskScheduler,
    @param:ApplicationContext private val context: Context,
) : SettingsItemRenderer {

    override fun handles(model: SettingsItemModel): Boolean =
        model.style == SettingsItemStyle.Custom &&
            (model as? SettingsItemModel.Custom)?.styleKey in
            setOf(STYLE_REFRESH_ACTIONS, STYLE_TIME_ACTIONS, STYLE_BACKUP_ACTIONS)

    @Composable
    override fun Render(
        model: SettingsItemModel,
        position: ItemPosition,
        enabled: Boolean,
        store: SettingsStore,
    ) {
        when ((model as SettingsItemModel.Custom).styleKey) {
            STYLE_REFRESH_ACTIONS -> RefreshActions(store)
            STYLE_TIME_ACTIONS -> TimeActions()
            STYLE_BACKUP_ACTIONS -> BackupActions()
        }
    }

    @Composable
    private fun RefreshActions(store: SettingsStore) {
        val overlay = LocalOverlayController.current
        val lastRunMillis by store.state(ConfundoSettings.lastRunMillis)
            .collectAsStateWithLifecycle()
        val locale = LocalLocale.current.platformLocale

        Column(
            modifier = Modifier.padding(horizontal = ArsenalTokens.spacing.lg, vertical = ArsenalTokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ArsenalTokens.spacing.sm),
        ) {
            Text(
                text = if (lastRunMillis > 0L) {
                    stringResource(
                        R.string.settings_last_run,
                        SimpleDateFormat("yyyy-MM-dd HH:mm", locale).format(Date(lastRunMillis)),
                    )
                } else {
                    stringResource(R.string.settings_last_run_never)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    taskScheduler.enqueueOneTime(RefreshTask.NAME)
                    overlay?.snackbar(context.getString(R.string.settings_run_now_done))
                },
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(R.string.settings_run_now))
            }
        }
    }

    @Composable
    private fun TimeActions() {
        val overlay = LocalOverlayController.current
        val scope = rememberCoroutineScope()

        fun promptClear(fieldKey: String, confirmMessageRes: Int, clearedMessageRes: Int) {
            overlay?.dialog(
                title = context.getString(R.string.settings_time_clear_dialog_title),
                message = context.getString(confirmMessageRes),
                confirmLabel = context.getString(R.string.dialog_confirm),
                dismissLabel = context.getString(R.string.dialog_cancel),
                onConfirm = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            configRepository.clearFieldForAll(fieldKey)
                        }
                        overlay.snackbar(context.getString(clearedMessageRes))
                    }
                },
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = ArsenalTokens.spacing.lg, vertical = ArsenalTokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ArsenalTokens.spacing.sm),
        ) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    promptClear(
                        FieldKeys.ACTIVATION_TIME,
                        R.string.settings_time_clear_activation_confirm,
                        R.string.settings_time_cleared_activation,
                    )
                },
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(R.string.settings_time_clear_activation))
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    promptClear(
                        FieldKeys.BOOT_TIME,
                        R.string.settings_time_clear_boot_confirm,
                        R.string.settings_time_cleared_boot,
                    )
                },
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(R.string.settings_time_clear_boot))
            }
        }
    }

    @Composable
    private fun BackupActions() {
        val overlay = LocalOverlayController.current
        val scope = rememberCoroutineScope()
        var busy by remember { mutableStateOf(false) }

        val exportLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            if (uri != null) {
                busy = true
                scope.launch {
                    runCatching { backupRepository.exportTo(uri) }
                        .onSuccess {
                            overlay?.snackbar(
                                context.getString(R.string.settings_backup_export_done),
                            )
                        }
                        .onFailure { e ->
                            overlay?.snackbar(
                                context.getString(
                                    R.string.settings_backup_export_failed,
                                    e.localizedMessage ?: e.javaClass.simpleName,
                                ),
                            )
                        }
                    busy = false
                }
            }
        }
        val importLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                overlay?.dialog(
                    title = context.getString(R.string.settings_backup_import_confirm_title),
                    message = context.getString(R.string.settings_backup_import_confirm_message),
                    confirmLabel = context.getString(R.string.dialog_confirm),
                    dismissLabel = context.getString(R.string.dialog_cancel),
                    onConfirm = {
                        busy = true
                        scope.launch {
                            runCatching { backupRepository.restoreFrom(uri) }
                                .onSuccess { summary ->
                                    overlay.snackbar(
                                        context.getString(
                                            R.string.settings_backup_import_done,
                                            summary.restoredApps,
                                            summary.skippedApps,
                                            summary.restoredTemplates,
                                        ),
                                    )
                                }
                                .onFailure { e ->
                                    overlay.snackbar(
                                        context.getString(
                                            R.string.settings_backup_import_failed,
                                            e.localizedMessage ?: e.javaClass.simpleName,
                                        ),
                                    )
                                }
                            busy = false
                        }
                    },
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = ArsenalTokens.spacing.lg, vertical = ArsenalTokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ArsenalTokens.spacing.sm),
        ) {
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                shapes = ButtonDefaults.shapes(),
                onClick = {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                        .format(Date())
                    exportLauncher.launch("Confundo_backup_$timestamp.json")
                },
            ) {
                if (busy) {
                    ArsenalLoadingIndicator(modifier = Modifier.size(ArsenalTokens.spacing.xl))
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ArsenalTokens.spacing.sm),
                    ) {
                        Icon(Icons.Rounded.Backup, contentDescription = null)
                        Text(stringResource(R.string.settings_backup_export))
                    }
                }
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                shapes = ButtonDefaults.shapes(),
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ArsenalTokens.spacing.sm),
                ) {
                    Icon(Icons.Rounded.Restore, contentDescription = null)
                    Text(stringResource(R.string.settings_backup_import))
                }
            }
        }
    }
}
