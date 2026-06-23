package killua.dev.confundo.ui.pages.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.FloatingToolbarDefaults.vibrantFloatingToolbarColors
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import killua.dev.confundo.R
import killua.dev.confundo.navigation.Routes
import killua.dev.confundo.ui.components.AppItemData
import killua.dev.confundo.ui.components.AppListRow
import killua.dev.confundo.ui.components.AppPosition
import killua.dev.confundo.ui.components.DialogButtonVariant
import killua.dev.confundo.ui.components.DialogVariant
import killua.dev.confundo.ui.components.ExpressiveRefreshIndicator
import killua.dev.confundo.ui.components.PageLoadingIndicator
import killua.dev.confundo.utils.LocalNavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomePage(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val navController = LocalNavController.current!!

    LaunchedEffect(Unit) { viewModel.emitIntentOnIO(HomeIntent.Load) }

    val selectedPkgs = remember { mutableStateListOf<String>() }
    val showToolbar = selectedPkgs.isNotEmpty()

    BackHandler(showToolbar) { selectedPkgs.clear() }

    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    var showMenu by remember { mutableStateOf(false) }

    val visibleApps = remember(state.apps, state.showSystemApps) {
        state.apps.filter { !it.isSystemApp || state.showSystemApps }
    }

    val scope = rememberCoroutineScope()

    var showApplyAllDialog by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }
    var templatePickerItems by remember { mutableStateOf<List<TemplateItem>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = null
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_show_system_apps)) },
                                onClick = {
                                    showMenu = false
                                    viewModel.emitIntentOnIO(HomeIntent.ToggleSystemApps)
                                },
                                leadingIcon = {
                                    Checkbox(
                                        checked = state.showSystemApps,
                                        onCheckedChange = null
                                    )
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state.phase) {
                HomePhase.Loading -> PageLoadingIndicator()
                HomePhase.Ready -> {
                    val pullState = rememberPullToRefreshState()
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = { viewModel.emitIntentOnIO(HomeIntent.Refresh) },
                        state = pullState,
                        modifier = Modifier.fillMaxSize(),
                        indicator = {
                            ExpressiveRefreshIndicator(
                                isRefreshing = state.isRefreshing,
                                state = pullState,
                                modifier = Modifier.align(Alignment.TopCenter),
                            )
                        },
                    ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = 88.dp
                        )
                    ) {
                        items(
                            state.apps,
                            key = { app -> app.packageName }
                        ) { app ->
                            val shouldShow = !app.isSystemApp || state.showSystemApps
                            androidx.compose.animation.AnimatedVisibility(
                                visible = shouldShow,
                                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
                            ) {
                                val index = visibleApps.indexOf(app)
                                if (index > 0) Spacer(Modifier.height(1.dp))
                                val position = when {
                                    visibleApps.size <= 1 -> AppPosition.Single
                                    index == 0 -> AppPosition.Top
                                    index == visibleApps.lastIndex -> AppPosition.Bottom
                                    else -> AppPosition.Middle
                                }
                                AppListRow(
                                    appData = AppItemData(
                                        id = app.packageName,
                                        icon = Icons.Rounded.Android,
                                        iconBitmap = app.iconBitmap,
                                        appName = app.appName,
                                        packageName = app.packageName,
                                        isSpoofingEnabled = app.isSpoofingEnabled,
                                    ),
                                    position = position,
                                    selected = app.packageName in selectedPkgs,
                                    onClick = {
                                        if (showToolbar) {
                                            if (app.packageName in selectedPkgs) {
                                                selectedPkgs.remove(app.packageName)
                                            } else {
                                                selectedPkgs.add(app.packageName)
                                            }
                                        } else {
                                            navController.navigate(Routes.appDetail(app.packageName))
                                        }
                                    },
                                    onLongClick = {
                                        if (app.packageName in selectedPkgs) {
                                            selectedPkgs.remove(app.packageName)
                                        } else {
                                            selectedPkgs.add(app.packageName)
                                        }
                                    },
                                )
                            }
                        }
                    }
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showToolbar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                HorizontalFloatingToolbar(
                    modifier = Modifier.padding(bottom = 16.dp),
                    expanded = true,
                    colors = vibrantFloatingToolbarColors(
                        toolbarContainerColor = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.85f),
                        toolbarContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    content = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .wrapContentWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilledIconButton(
                                    onClick = { selectedPkgs.clear() },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Deselect,
                                        stringResource(R.string.toolbar_deselect_all)
                                    )
                                }
                                FilledIconButton(
                                    onClick = {
                                        selectedPkgs.clear()
                                        selectedPkgs.addAll(visibleApps.map { it.packageName })
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.SelectAll,
                                        stringResource(R.string.toolbar_select_all)
                                    )
                                }

                                FilledIconButton(
                                    onClick = {
                                        val pkgs = selectedPkgs.toList()
                                        selectedPkgs.clear()
                                        viewModel.emitIntentOnIO(HomeIntent.BatchSetEnabled(pkgs, true))
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    )
                                ) {
                                    Icon(
                                        Icons.Rounded.PlayArrow,
                                        stringResource(R.string.toolbar_enable_all)
                                    )
                                }
                                FilledIconButton(
                                    onClick = {
                                        val pkgs = selectedPkgs.toList()
                                        selectedPkgs.clear()
                                        viewModel.emitIntentOnIO(HomeIntent.BatchSetEnabled(pkgs, false))
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                    )
                                ) {
                                    Icon(
                                        Icons.Rounded.Stop,
                                        stringResource(R.string.toolbar_disable_all),
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        val pkgs = selectedPkgs.toList()
                                        selectedPkgs.clear()
                                        viewModel.emitIntentOnIO(HomeIntent.BatchSetAutoReset(pkgs, true))
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    )
                                ) {
                                    Text(stringResource(R.string.toolbar_reset_on))
                                }
                                FilledTonalButton(
                                    onClick = {
                                        val pkgs = selectedPkgs.toList()
                                        selectedPkgs.clear()
                                        viewModel.emitIntentOnIO(HomeIntent.BatchSetAutoReset(pkgs, false))
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    )
                                ) {
                                    Text(stringResource(R.string.toolbar_reset_off))
                                }

                                FilledTonalButton(
                                    onClick = {
                                        scope.launch {
                                            val templates = viewModel.loadTemplates()
                                            if (templates.isNotEmpty()) {
                                                templatePickerItems = templates
                                                showTemplatePicker = true
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    )
                                ) {
                                    Text(stringResource(R.string.toolbar_apply_template))
                                }
                            }
                        }
                    },
                )
            }

            FloatingActionButtonMenu(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                expanded = fabMenuExpanded,
                button = {
                    ToggleFloatingActionButton(
                        checked = fabMenuExpanded,
                        onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
                        containerColor = ToggleFloatingActionButtonDefaults.containerColor(
                            initialColor = MaterialTheme.colorScheme.secondaryContainer,
                            finalColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Icon(
                            if (fabMenuExpanded) Icons.Filled.Close else Icons.Filled.Menu,
                            stringResource(R.string.fab_template_manage)
                        )
                    }
                },
            ) {
                FloatingActionButtonMenuItem(
                    icon = { Icon(Icons.Filled.Apps, contentDescription = null) },
                    text = { Text(stringResource(R.string.fab_apply_all)) },
                    onClick = {
                        fabMenuExpanded = false
                        showApplyAllDialog = true
                    }
                )
                FloatingActionButtonMenuItem(
                    icon = { Icon(Icons.Filled.Menu, contentDescription = null) },
                    text = { Text(stringResource(R.string.fab_template_manage)) },
                    onClick = {
                        fabMenuExpanded = false
                        navController.navigate(Routes.TEMPLATE_MANAGE)
                    }
                )
            }
        }
    }

    if (showApplyAllDialog) {
        AlertDialog(
            onDismissRequest = { showApplyAllDialog = false },
            title = {
                Text(stringResource(R.string.dialog_apply_all_title))
            },
            text = {
                Text(stringResource(R.string.dialog_apply_all_message))
            },
            confirmButton = {
                TextButton(onClick = {
                    showApplyAllDialog = false
                    viewModel.emitIntentOnIO(HomeIntent.ApplyToAll)
                }) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showApplyAllDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    if (showTemplatePicker) {
        TemplatePickerDialog(
            templates = templatePickerItems,
            onSelect = { templateId ->
                showTemplatePicker = false
                val pkgs = selectedPkgs.toList()
                selectedPkgs.clear()
                viewModel.emitIntentOnIO(HomeIntent.ApplyTemplate(pkgs, templateId))
            },
            onDismiss = { showTemplatePicker = false }
        )
    }
}

@Composable
private fun TemplatePickerDialog(
    templates: List<TemplateItem>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    DialogVariant(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.template_apply_dialog_title))
        },
        buttons = {
            templates.forEach { template ->
                DialogButtonVariant(
                    text = template.name,
                    surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    textColor = MaterialTheme.colorScheme.onSurface,
                    onClick = { onSelect(template.id) },
                )
            }
        }
    )
}
