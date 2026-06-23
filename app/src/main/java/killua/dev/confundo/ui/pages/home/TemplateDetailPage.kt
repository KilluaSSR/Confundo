package killua.dev.confundo.ui.pages.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import killua.dev.confundo.R
import killua.dev.confundo.data.FieldCatalog
import killua.dev.confundo.data.FieldSpec
import killua.dev.confundo.ui.components.AppDetailItem
import killua.dev.confundo.ui.components.FieldInputDialog
import killua.dev.confundo.ui.components.PageLoadingIndicator
import killua.dev.confundo.ui.components.SectionHeader
import killua.dev.confundo.ui.components.TextInputDialog
import killua.dev.confundo.utils.LocalNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDetailPage(
    templateId: String,
    viewModel: TemplateDetailViewModel = hiltViewModel()
) {
    val navController = LocalNavController.current!!
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(templateId) { viewModel.emitIntentOnIO(TemplateDetailIntent.Load(templateId)) }

    var editingSpec by remember { mutableStateOf<FieldSpec?>(null) }
    var editingTitle by remember { mutableStateOf("") }
    var editingValue by remember { mutableStateOf("") }
    var editingName by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(state.name.ifEmpty { if (state.isNew) stringResource(R.string.template_new) else "" })
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.emitIntentOnIO(TemplateDetailIntent.RandomFill) }) {
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

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clickable { editingName = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 1.dp,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = state.name.ifEmpty { stringResource(R.string.template_new) },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (state.name.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

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

    if (editingName) {
        TextInputDialog(
            show = true,
            title = stringResource(R.string.template_new),
            initialValue = state.name,
            confirmText = stringResource(R.string.dialog_confirm),
            dismissText = stringResource(R.string.dialog_cancel),
            onConfirm = { value ->
                editingName = false
                viewModel.emitIntentOnIO(TemplateDetailIntent.SetName(value))
            },
            onDismiss = { editingName = false }
        )
    }

    FieldInputDialog(
        spec = editingSpec,
        title = editingTitle,
        initialValue = editingValue,
        onConfirm = { value ->
            editingSpec?.let { viewModel.emitIntentOnIO(TemplateDetailIntent.UpdateField(it.key, value)) }
            editingSpec = null
        },
        onDismiss = { editingSpec = null }
    )
}
