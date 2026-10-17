package killua.dev.confundo.ui.pages.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import killua.dev.arsenal.material.templates.basics.LoadingView
import killua.dev.arsenal.material.templates.settings.SettingsItem
import killua.dev.arsenal.navigation.annotation.NavDestination
import killua.dev.confundo.R
import killua.dev.confundo.data.FieldCatalog
import killua.dev.confundo.data.FieldSpec
import killua.dev.confundo.ui.components.DetailScaffold
import killua.dev.confundo.ui.components.FieldInputDialog
import killua.dev.confundo.ui.components.RowGroup
import killua.dev.confundo.ui.components.TextInputDialog
import killua.dev.confundo.ui.theme.Dimens
import killua.dev.confundo.ui.theme.ShapeRadius
import killua.dev.confundo.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@NavDestination(route = "template_detail/{templateId}")
@Composable
fun TemplateDetailPage(backStackEntry: NavBackStackEntry) {
    val viewModel: TemplateDetailViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val templateId = backStackEntry.arguments?.getString("templateId") ?: "new"

    LaunchedEffect(templateId) { viewModel.dispatch(TemplateDetailIntent.Load(templateId)) }


    var editingSpec by remember { mutableStateOf<FieldSpec?>(null) }
    var editingTitle by remember { mutableStateOf("") }
    var editingValue by remember { mutableStateOf("") }
    var editingName by remember { mutableStateOf(false) }

    DetailScaffold(
        title = state.name.ifEmpty {
            if (state.isNew) stringResource(R.string.template_new) else ""
        },
        onBack = { viewModel.dispatch(TemplateDetailIntent.Back) },
        actions = {
            IconButton(
                onClick = { viewModel.dispatch(TemplateDetailIntent.RandomFill) },
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.menu_random_fill),
                )
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state.phase) {
                DetailPhase.Loading -> LoadingView()
                DetailPhase.Ready -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .widthIn(max = Dimens.ContentMaxWidth)
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                            )
                            .padding(horizontal = Spacing.lg)
                            .align(Alignment.TopCenter)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingName = true },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                ShapeRadius.Large
                            ),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                                    contentDescription = stringResource(R.string.cd_edit_name),
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        FieldCatalog.grouped.forEach { (category, specs) ->
                            RowGroup(
                                title = stringResource(category.titleRes),
                                rows = specs.map { spec ->
                                    { slot ->
                                        val title = stringResource(spec.labelRes)
                                        SettingsItem(
                                            title = title,
                                            value = state.fields[spec.key] ?: "",
                                            onClick = {
                                                editingSpec = spec
                                                editingTitle = title
                                                editingValue = state.fields[spec.key] ?: ""
                                            },
                                            shape = slot.shape,
                                            containerColor = slot.color,
                                        )
                                    }
                                },
                            )
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
                viewModel.dispatch(TemplateDetailIntent.SetName(value))
            },
            onDismiss = { editingName = false }
        )
    }

    FieldInputDialog(
        spec = editingSpec,
        title = editingTitle,
        initialValue = editingValue,
        onConfirm = { value ->
            editingSpec?.let { viewModel.dispatch(TemplateDetailIntent.UpdateField(it.key, value)) }
            editingSpec = null
        },
        onDismiss = { editingSpec = null }
    )
}
