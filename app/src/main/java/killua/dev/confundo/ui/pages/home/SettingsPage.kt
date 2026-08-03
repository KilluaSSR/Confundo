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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.ui.platform.LocalLocale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.emitIntentOnIO(SettingsIntent.Load) }

    val snackbarHostState = remember { SnackbarHostState() }
    ObserveSnackbarEffects(viewModel.effects, snackbarHostState)

    SettingsPageContent(state, snackbarHostState, onIntent = viewModel::emitIntentOnIO)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPageContent(
    state: SettingsUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onIntent: (SettingsIntent) -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                scrollBehavior = scrollBehavior,
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
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                RefreshSection(state, onIntent)
                TimeFieldsSection(state, onIntent)
                BackupSection(onIntent)
                AppearanceSection(state, onIntent)
                Spacer(Modifier.height(Spacing.lg))
            }
        }
    }
}

private enum class ClearTarget { ACTIVATION, BOOT }

@Composable
private fun TimeFieldsSection(state: SettingsUiState, onIntent: (SettingsIntent) -> Unit) {
    var pendingClear by remember { mutableStateOf<ClearTarget?>(null) }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column {
            SectionHeader(title = stringResource(R.string.settings_time_title))

            CardSwitch(
                text = stringResource(R.string.settings_time_randomize_activation),
                checked = state.randomizeActivationTime,
                onCheckedChange = { onIntent(SettingsIntent.SetRandomizeActivationTime(it)) },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            CardSwitch(
                text = stringResource(R.string.settings_time_randomize_boot),
                checked = state.randomizeBootTime,
                onCheckedChange = { onIntent(SettingsIntent.SetRandomizeBootTime(it)) },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            Text(
                text = stringResource(R.string.settings_time_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )

            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { pendingClear = ClearTarget.ACTIVATION },
                ) {
                    Text(stringResource(R.string.settings_time_clear_activation))
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { pendingClear = ClearTarget.BOOT },
                ) {
                    Text(stringResource(R.string.settings_time_clear_boot))
                }
            }
            Spacer(Modifier.height(8.dp))
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
                TextButton(onClick = {
                    onIntent(intent)
                    pendingClear = null
                }) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingClear = null }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

@Composable
private fun BackupSection(onIntent: (SettingsIntent) -> Unit) {
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { onIntent(SettingsIntent.ExportBackup(it)) } }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> pendingImportUri = uri }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column {
            SectionHeader(title = stringResource(R.string.settings_backup_title))

            Text(
                text = stringResource(R.string.settings_backup_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )

            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                            .format(Date())
                        exportLauncher.launch("Confundo_backup_$timestamp.json")
                    },
                ) {
                    Text(stringResource(R.string.settings_backup_export))
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                ) {
                    Text(stringResource(R.string.settings_backup_import))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text(stringResource(R.string.settings_backup_import_confirm_title)) },
            text = { Text(stringResource(R.string.settings_backup_import_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onIntent(SettingsIntent.ImportBackup(uri))
                    pendingImportUri = null
                }) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

@Composable
private fun RefreshSection(state: SettingsUiState, onIntent: (SettingsIntent) -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
            CardSwitch(
                text = stringResource(R.string.settings_auto_refresh_title),
                checked = state.autoRefreshEnabled,
                onCheckedChange = { onIntent(SettingsIntent.SetAutoRefresh(it)) },
            )
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = stringResource(R.string.settings_interval_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.settings_interval_days, state.intervalDays),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
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
                        .padding(vertical = 12.dp),
                    onClick = { onIntent(SettingsIntent.RunNow) }
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
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column {
            SectionHeader(title = stringResource(R.string.settings_appearance_title))

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = stringResource(R.string.settings_dark_mode_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
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
                Spacer(Modifier.height(8.dp))
            }

            CardSwitch(
                text = stringResource(R.string.settings_dynamic_color_title),
                checked = state.dynamicColor,
                onCheckedChange = { onIntent(SettingsIntent.SetDynamicColor(it)) },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            Text(
                text = stringResource(R.string.settings_dynamic_color_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
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
