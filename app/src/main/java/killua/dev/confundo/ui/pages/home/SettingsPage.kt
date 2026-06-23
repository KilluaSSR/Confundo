package killua.dev.confundo.ui.pages.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import killua.dev.confundo.R
import killua.dev.confundo.data.AppSettings
import killua.dev.confundo.ui.components.CardSwitch
import killua.dev.confundo.ui.theme.ConfundoTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.emitIntentOnIO(SettingsIntent.Load) }
    SettingsPageContent(state, onIntent = viewModel::emitIntentOnIO)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPageContent(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.nav_settings),
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Refresh(state, onIntent)
        }
    }
}

@Composable
private fun Refresh(state: SettingsUiState, onIntent: (SettingsIntent) -> Unit = {}) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            CardSwitch(
                text = stringResource(R.string.settings_auto_refresh_title),
                checked = state.autoRefreshEnabled,
                onCheckedChange = {
                    onIntent(SettingsIntent.SetAutoRefresh(it))
                },
            )
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_interval_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        R.string.settings_interval_days,
                        state.intervalDays
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = state.intervalDays.toFloat(),
                    onValueChange = {
                        onIntent(SettingsIntent.SetInterval(it.toInt()))
                    },
                    valueRange = AppSettings.MIN_INTERVAL_DAYS.toFloat()..
                            AppSettings.MAX_INTERVAL_DAYS.toFloat(),
                    steps = AppSettings.MAX_INTERVAL_DAYS -
                            AppSettings.MIN_INTERVAL_DAYS - 1,
                    enabled = state.autoRefreshEnabled,
                )
                Text(
                    text = stringResource(
                        R.string.settings_last_run,
                        SimpleDateFormat("yyyy-MM-dd HH:mm", LocalLocale.current.platformLocale).format(Date(state.lastRunMillis))),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    onClick = {
                        onIntent(SettingsIntent.RunNow)
                    }
                ) {
                    Text(stringResource(R.string.settings_run_now))
                }
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