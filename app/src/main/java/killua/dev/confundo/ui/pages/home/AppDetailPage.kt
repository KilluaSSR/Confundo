package killua.dev.confundo.ui.pages.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import killua.dev.confundo.R
import killua.dev.confundo.data.FieldCatalog
import killua.dev.confundo.data.FieldSpec
import killua.dev.confundo.ui.components.AppDetailItem
import killua.dev.confundo.ui.components.CardSwitch
import killua.dev.confundo.ui.components.FieldInputDialog
import killua.dev.confundo.ui.components.PageLoadingIndicator
import killua.dev.confundo.ui.components.SectionHeader
import killua.dev.confundo.utils.LocalNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailPage(pkg: String, viewModel: AppDetailViewModel = hiltViewModel()) {
    val navController = LocalNavController.current!!
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(pkg) { viewModel.emitIntentOnIO(AppDetailIntent.Load(pkg)) }

    var editingSpec by remember { mutableStateOf<FieldSpec?>(null) }
    var editingTitle by remember { mutableStateOf("") }
    var editingValue by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.appName.ifEmpty { pkg }) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.emitIntentOnIO(AppDetailIntent.RandomFill) }) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.menu_random_fill)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state.phase) {
                DetailPhase.Loading -> PageLoadingIndicator()
                DetailPhase.Ready -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        CardSwitch(
                            text = stringResource(R.string.switch_enable),
                            checked = state.enabled,
                            onCheckedChange = { viewModel.emitIntentOnIO(AppDetailIntent.SetEnabled(it)) },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        CardSwitch(
                            text = stringResource(R.string.switch_auto_reset),
                            checked = state.autoReset,
                            onCheckedChange = if (state.enabled) {
                                { viewModel.emitIntentOnIO(AppDetailIntent.SetAutoReset(it)) }
                            } else null,
                            enabled = state.enabled,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        FieldCatalog.grouped.forEach { (category, specs) ->
                            SectionHeader(title = stringResource(category.titleRes))
                            specs.forEach { spec ->
                                val title = stringResource(spec.labelRes)
                                AppDetailItem(
                                    title = title,
                                    content = state.fields[spec.key] ?: "",
                                    enabled = state.enabled,
                                    onClick = {
                                        editingSpec = spec
                                        editingTitle = title
                                        editingValue = state.fields[spec.key] ?: ""
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    FieldInputDialog(
        spec = editingSpec,
        title = editingTitle,
        initialValue = editingValue,
        onConfirm = { value ->
            editingSpec?.let { viewModel.emitIntentOnIO(AppDetailIntent.UpdateField(it.key, value)) }
            editingSpec = null
        },
        onDismiss = { editingSpec = null }
    )
}
