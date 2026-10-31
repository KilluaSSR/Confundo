package killua.dev.confundo.ui.pages.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingToolbarDefaults.vibrantFloatingToolbarColors
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import killua.dev.arsenal.material.ArsenalTokens
import killua.dev.arsenal.material.overlay.LocalOverlayController
import killua.dev.arsenal.material.templates.basics.EmptyView
import killua.dev.arsenal.material.templates.basics.LoadingView
import killua.dev.arsenal.material.templates.settings.SettingsItem
import killua.dev.arsenal.material.templates.settings.groupedItemShape
import killua.dev.arsenal.material.templates.settings.itemPosition
import killua.dev.arsenal.navigation.annotation.NavDestination
import killua.dev.confundo.R
import killua.dev.confundo.ui.components.ConfundoSurfaces
import killua.dev.confundo.ui.components.DetailScaffold

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@NavDestination(route = "template_manage")
@Composable
fun TemplateManagePage() {
    val viewModel: TemplateManageViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val overlay = LocalOverlayController.current

    LaunchedEffect(Unit) { viewModel.dispatch(TemplateManageIntent.Load) }

    val selectedIds = remember { mutableStateListOf<String>() }
    val showToolbar = selectedIds.isNotEmpty()

    BackHandler(showToolbar) { selectedIds.clear() }

    val deleteTitle = stringResource(R.string.dialog_delete_template_title)
    val deleteMessage = stringResource(R.string.dialog_delete_template_message, selectedIds.size)
    val deleteLabel = stringResource(R.string.dialog_delete)
    val cancelLabel = stringResource(R.string.dialog_cancel)

    DetailScaffold(
        title = stringResource(R.string.template_manage),
        onBack = { viewModel.dispatch(TemplateManageIntent.Back) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.template_new)) },
                icon = {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.cd_new_template),
                    )
                },
                onClick = { viewModel.dispatch(TemplateManageIntent.OpenTemplateDetail("new")) },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (state.phase) {
                TemplateManagePhase.Loading -> LoadingView()
                TemplateManagePhase.Ready -> {
                    if (state.templates.isEmpty()) {
                        EmptyView(
                            icon = Icons.Outlined.Dashboard,
                            title = stringResource(R.string.template_no_templates),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = ArsenalTokens.spacing.md,
                                end = ArsenalTokens.spacing.md,
                                top = ArsenalTokens.spacing.sm,
                                bottom = 96.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            itemsIndexed(
                                state.templates,
                                key = { _, t -> t.id },
                            ) { index, template ->
                                val selected = template.id in selectedIds
                                SettingsItem(
                                    title = template.name,
                                    onClick = {
                                        if (showToolbar) {
                                            if (selected) selectedIds.remove(template.id)
                                            else selectedIds.add(template.id)
                                        } else {
                                            viewModel.dispatch(
                                                TemplateManageIntent.OpenTemplateDetail(template.id),
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        if (selected) selectedIds.remove(template.id)
                                        else selectedIds.add(template.id)
                                    },
                                    shape = groupedItemShape(itemPosition(index, state.templates.size)),
                                    containerColor = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        ConfundoSurfaces.row
                                    },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showToolbar,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                ) { it } + fadeIn(spring(stiffness = Spring.StiffnessMedium)),
                exit = slideOutVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                ) { it } + fadeOut(spring(stiffness = Spring.StiffnessMedium)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
            ) {
                HorizontalFloatingToolbar(
                    modifier = Modifier.padding(bottom = 16.dp),
                    expanded = true,
                    colors = vibrantFloatingToolbarColors(),
                    content = {
                        Row(
                            Modifier
                                .wrapContentWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            FilledIconButton(
                                onClick = { selectedIds.clear() },
                                shapes = IconButtonDefaults.shapes(),
                            ) {
                                Icon(
                                    Icons.Default.Deselect,
                                    stringResource(R.string.toolbar_deselect_all),
                                )
                            }
                            FilledIconButton(
                                onClick = {
                                    selectedIds.clear()
                                    selectedIds.addAll(state.templates.map { it.id })
                                },
                                shapes = IconButtonDefaults.shapes(),
                            ) {
                                Icon(
                                    Icons.Default.SelectAll,
                                    stringResource(R.string.toolbar_select_all),
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    overlay?.dialog(
                                        title = deleteTitle,
                                        message = deleteMessage,
                                        confirmLabel = deleteLabel,
                                        dismissLabel = cancelLabel,
                                        onConfirm = {
                                            val ids = selectedIds.toList()
                                            selectedIds.clear()
                                            viewModel.dispatch(
                                                TemplateManageIntent.DeleteSelected(ids),
                                            )
                                        },
                                    )
                                },
                                shapes = ButtonDefaults.shapes(),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                ),
                            ) {
                                Text(stringResource(R.string.toolbar_delete_selected))
                            }
                        }
                    },
                )
            }
        }
    }
}
