package killua.dev.confundo.ui.pages.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import killua.dev.confundo.R
import killua.dev.confundo.data.AppSettings
import killua.dev.confundo.ui.components.CardSwitch
import killua.dev.confundo.ui.components.ObserveSnackbarEffects
import killua.dev.confundo.ui.components.SectionHeader
import killua.dev.confundo.ui.theme.ConfundoTheme
import killua.dev.confundo.ui.theme.Dimens
import killua.dev.confundo.ui.theme.Spacing
import killua.dev.confundo.ui.theme.ShapeRadius
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.ui.platform.LocalLocale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsPage(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.emitIntentOnIO(SettingsIntent.Load) }

    val snackbarHostState = remember { SnackbarHostState() }
    ObserveSnackbarEffects(viewModel.effects, snackbarHostState)

    SettingsPageContent(state, snackbarHostState, onIntent = viewModel::emitIntentOnIO)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsPageContent(
    state: SettingsUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onIntent: (SettingsIntent) -> Unit = {},
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .widthIn(max = Dimens.ContentMaxWidth)
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                    )
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                RefreshSection(state, onIntent)
                TimeFieldsSection(state, onIntent)
                NativeHookSection(state, onIntent)
                BackupSection(state.backupInProgress, onIntent)
                AppearanceSection(state, onIntent)
                Spacer(Modifier.height(Spacing.lg))
            }
        }
    }
}

private enum class ClearTarget { ACTIVATION, BOOT }
private enum class SettingsItemPosition { Single, Top, Middle, Bottom }

private fun settingsItemShape(position: SettingsItemPosition): Shape = when (position) {
    SettingsItemPosition.Single -> RoundedCornerShape(ShapeRadius.Large)
    SettingsItemPosition.Top -> RoundedCornerShape(
        topStart = ShapeRadius.Large,
        topEnd = ShapeRadius.Large,
        bottomStart = ShapeRadius.ExtraSmall,
        bottomEnd = ShapeRadius.ExtraSmall,
    )
    SettingsItemPosition.Middle -> RoundedCornerShape(ShapeRadius.ExtraSmall)
    SettingsItemPosition.Bottom -> RoundedCornerShape(
        topStart = ShapeRadius.ExtraSmall,
        topEnd = ShapeRadius.ExtraSmall,
        bottomStart = ShapeRadius.Large,
        bottomEnd = ShapeRadius.Large,
    )
}

@Composable
private fun TimeFieldsSection(state: SettingsUiState, onIntent: (SettingsIntent) -> Unit) {
    var pendingClear by remember { mutableStateOf<ClearTarget?>(null) }

    Column {
        SectionHeader(title = stringResource(R.string.settings_time_title), startPadding = 0.dp)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            CardSwitch(
                text = stringResource(R.string.settings_time_randomize_activation),
                checked = state.randomizeActivationTime,
                onCheckedChange = { onIntent(SettingsIntent.SetRandomizeActivationTime(it)) },
                containerColor = MaterialTheme.colorScheme.surfaceBright,
                shape = settingsItemShape(SettingsItemPosition.Top),
            )
            CardSwitch(
                text = stringResource(R.string.settings_time_randomize_boot),
                checked = state.randomizeBootTime,
                onCheckedChange = { onIntent(SettingsIntent.SetRandomizeBootTime(it)) },
                containerColor = MaterialTheme.colorScheme.surfaceBright,
                shape = settingsItemShape(SettingsItemPosition.Middle),
            )
            Surface(
                shape = settingsItemShape(SettingsItemPosition.Bottom),
                color = MaterialTheme.colorScheme.surfaceBright,
            ) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Text(
                        text = stringResource(R.string.settings_time_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { pendingClear = ClearTarget.ACTIVATION },
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Text(stringResource(R.string.settings_time_clear_activation))
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { pendingClear = ClearTarget.BOOT },
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Text(stringResource(R.string.settings_time_clear_boot))
                    }
                }
            }
        }
    }

    pendingClear?.let { target ->
        val (bodyRes, intent) = when (target) {
            ClearTarget.ACTIVATION ->
                R.string.settings_time_clear_activation_confirm to SettingsIntent.ClearAllActivationTime
            ClearTarget.BOOT ->
                R.string.settings_time_clear_boot_confirm to SettingsIntent.ClearAllBootTime
        }
        AlertDialog(
            onDismissRequest = { pendingClear = null },
            title = { Text(stringResource(R.string.settings_time_clear_dialog_title)) },
            text = { Text(stringResource(bodyRes)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onIntent(intent)
                        pendingClear = null
                    },
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingClear = null },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

@Composable
private fun NativeHookSection(state: SettingsUiState, onIntent: (SettingsIntent) -> Unit) {
    Column {
        SectionHeader(title = stringResource(R.string.settings_native_title), startPadding = 0.dp)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            CardSwitch(
                text = stringResource(R.string.settings_native_hook_title),
                checked = state.nativeHookEnabled,
                onCheckedChange = { onIntent(SettingsIntent.SetNativeHook(it)) },
                containerColor = MaterialTheme.colorScheme.surfaceBright,
                shape = settingsItemShape(SettingsItemPosition.Top),
            )
            Surface(
                shape = settingsItemShape(SettingsItemPosition.Bottom),
                color = MaterialTheme.colorScheme.surfaceBright,
            ) {
                Text(
                    text = stringResource(R.string.settings_native_hook_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.lg),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BackupSection(
    inProgress: Boolean,
    onIntent: (SettingsIntent) -> Unit,
) {
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { onIntent(SettingsIntent.ExportBackup(it)) } }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> pendingImportUri = uri }

    Column {
        SectionHeader(title = stringResource(R.string.settings_backup_title), startPadding = 0.dp)
        Surface(
            shape = settingsItemShape(SettingsItemPosition.Single),
            color = MaterialTheme.colorScheme.surfaceBright,
        ) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Text(
                    text = stringResource(R.string.settings_backup_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Column(
                    modifier = Modifier.padding(top = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !inProgress,
                        shapes = ButtonDefaults.shapes(),
                        onClick = {
                            val timestamp =
                                SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                                    .format(Date())
                            exportLauncher.launch("Confundo_backup_$timestamp.json")
                        },
                    ) {
                        if (inProgress) {
                            LoadingIndicator(modifier = Modifier.size(Spacing.xl))
                        } else {
                            Text(stringResource(R.string.settings_backup_export))
                        }
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !inProgress,
                        shapes = ButtonDefaults.shapes(),
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    ) {
                        Text(stringResource(R.string.settings_backup_import))
                    }
                }
            }
        }
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text(stringResource(R.string.settings_backup_import_confirm_title)) },
            text = { Text(stringResource(R.string.settings_backup_import_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onIntent(SettingsIntent.ImportBackup(uri))
                        pendingImportUri = null
                    },
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingImportUri = null },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

@Composable
private fun RefreshSection(state: SettingsUiState, onIntent: (SettingsIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        CardSwitch(
            text = stringResource(R.string.settings_auto_refresh_title),
            checked = state.autoRefreshEnabled,
            onCheckedChange = { onIntent(SettingsIntent.SetAutoRefresh(it)) },
            containerColor = MaterialTheme.colorScheme.surfaceBright,
            shape = settingsItemShape(SettingsItemPosition.Top),
        )
        Surface(
            shape = settingsItemShape(SettingsItemPosition.Bottom),
            color = MaterialTheme.colorScheme.surfaceBright,
        ) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Text(
                    text = stringResource(R.string.settings_interval_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = stringResource(R.string.settings_interval_days, state.intervalDays),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(Spacing.xs))
                Slider(
                    value = state.intervalDays.toFloat(),
                    onValueChange = { onIntent(SettingsIntent.SetInterval(it.toInt())) },
                    valueRange = AppSettings.MIN_INTERVAL_DAYS.toFloat()..
                            AppSettings.MAX_INTERVAL_DAYS.toFloat(),
                    steps = AppSettings.MAX_INTERVAL_DAYS - AppSettings.MIN_INTERVAL_DAYS - 1,
                    enabled = state.autoRefreshEnabled,
                    modifier = Modifier.semantics {
                        stateDescription = "${state.intervalDays}"
                    },
                )
                Text(
                    text = stringResource(
                        R.string.settings_last_run,
                        SimpleDateFormat(
                            "yyyy-MM-dd HH:mm",
                            LocalLocale.current.platformLocale
                        ).format(Date(state.lastRunMillis))
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.md),
                    onClick = { onIntent(SettingsIntent.RunNow) },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(R.string.settings_run_now))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSection(state: SettingsUiState, onIntent: (SettingsIntent) -> Unit) {
    Column {
        SectionHeader(title = stringResource(R.string.settings_appearance_title), startPadding = 0.dp)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Surface(
                shape = settingsItemShape(SettingsItemPosition.Top),
                color = MaterialTheme.colorScheme.surfaceBright,
            ) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Text(
                        text = stringResource(R.string.settings_dark_mode_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    val options = listOf(
                        AppSettings.DARK_MODE_SYSTEM to R.string.settings_dark_mode_system,
                        AppSettings.DARK_MODE_LIGHT to R.string.settings_dark_mode_light,
                        AppSettings.DARK_MODE_DARK to R.string.settings_dark_mode_dark,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        options.forEachIndexed { index, (mode, labelRes) ->
                            SegmentedButton(
                                selected = state.darkMode == mode,
                                onClick = { onIntent(SettingsIntent.SetDarkMode(mode)) },
                                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                            ) {
                                Text(stringResource(labelRes))
                            }
                        }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                }
            }

            CardSwitch(
                text = stringResource(R.string.settings_dynamic_color_title),
                checked = state.dynamicColor,
                onCheckedChange = { onIntent(SettingsIntent.SetDynamicColor(it)) },
                containerColor = MaterialTheme.colorScheme.surfaceBright,
                shape = settingsItemShape(SettingsItemPosition.Middle),
            )
            Surface(
                shape = settingsItemShape(SettingsItemPosition.Bottom),
                color = MaterialTheme.colorScheme.surfaceBright,
            ) {
                Text(
                    text = stringResource(R.string.settings_dynamic_color_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.lg),
                )
            }
        }
    }
}

@Preview
@Composable
private fun SettingsPagePreview() {
    ConfundoTheme {
        SettingsPageContent(
            state = SettingsUiState(
                autoRefreshEnabled = true,
                intervalDays = 7,
                lastRunMillis = System.currentTimeMillis()
            )
        )
    }
}
