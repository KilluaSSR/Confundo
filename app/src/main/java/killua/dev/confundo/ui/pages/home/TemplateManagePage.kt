package killua.dev.confundo.ui.pages.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults.vibrantFloatingToolbarColors
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import killua.dev.confundo.R
import killua.dev.confundo.navigation.Routes
import killua.dev.confundo.ui.components.AppPosition
import killua.dev.confundo.ui.components.ObserveSnackbarEffects
import killua.dev.confundo.ui.components.PageLoadingIndicator
import killua.dev.confundo.ui.components.TemplateListRow
import killua.dev.confundo.utils.LocalNavController

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TemplateManagePage(viewModel: TemplateManageViewModel = hiltViewModel()) {
    val navController = LocalNavController.current!!
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.emitIntentOnIO(TemplateManageIntent.Load) }

    val snackbarHostState = remember { SnackbarHostState() }
    ObserveSnackbarEffects(viewModel.effects, snackbarHostState)

    val selectedIds = remember { mutableStateListOf<String>() }
    val showToolbar = selectedIds.isNotEmpty()

    BackHandler(showToolbar) { selectedIds.clear() }

    var showDeleteDialog by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.template_manage)) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.templateDetail("new")) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_new_template)
                )
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                )
        ) {
            when (state.phase) {
                TemplateManagePhase.Loading -> PageLoadingIndicator()
                TemplateManagePhase.Ready -> {
                    if (state.templates.isEmpty()) {
                        EmptyTemplates(
                            onCreate = { navController.navigate(Routes.templateDetail("new")) }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            itemsIndexed(
                                state.templates,
                                key = { _, t -> t.id }
                            ) { index, template ->
                                val position = when {
                                    state.templates.size <= 1 -> AppPosition.Single
                                    index == 0 -> AppPosition.Top
                                    index == state.templates.lastIndex -> AppPosition.Bottom
                                    else -> AppPosition.Middle
                                }
                                TemplateListRow(
                                    name = template.name,
                                    position = position,
                                    selected = template.id in selectedIds,
                                    onClick = {
                                        if (showToolbar) {
                                            if (template.id in selectedIds) {
                                                selectedIds.remove(template.id)
                                            } else {
                                                selectedIds.add(template.id)
                                            }
                                        } else {
                                            navController.navigate(Routes.templateDetail(template.id))
                                        }
                                    },
                                    onLongClick = {
                                        if (template.id in selectedIds) {
                                            selectedIds.remove(template.id)
                                        } else {
                                            selectedIds.add(template.id)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showToolbar,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    )
                ) { it } + fadeIn(spring(stiffness = Spring.StiffnessMedium)),
                exit = slideOutVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium,
                    )
                ) { it } + fadeOut(spring(stiffness = Spring.StiffnessMedium)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
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
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilledIconButton(
                                onClick = { selectedIds.clear() },
                                shapes = IconButtonDefaults.shapes(),
                            ) {
                                Icon(
                                    Icons.Default.Deselect,
                                    stringResource(R.string.toolbar_deselect_all)
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
                                    stringResource(R.string.toolbar_select_all)
                                )
                            }
                            FilledTonalButton(
                                onClick = { showDeleteDialog = true },
                                shapes = ButtonDefaults.shapes(),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                )
                            ) {
                                Text(stringResource(R.string.toolbar_delete_selected))
                            }
                        }
                    },
                )
            }
        }
    }

    if (showDeleteDialog) {
        val count = selectedIds.size
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_template_title)) },
            text = { Text(stringResource(R.string.dialog_delete_template_message, count)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        val ids = selectedIds.toList()
                        selectedIds.clear()
                        viewModel.emitIntentOnIO(TemplateManageIntent.DeleteSelected(ids))
                    },
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.dialog_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun EmptyTemplates(onCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Dashboard,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.template_no_templates),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(
            onClick = onCreate,
            shapes = ButtonDefaults.shapes(),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.template_empty_action))
        }
    }
}
