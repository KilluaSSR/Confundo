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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingToolbarDefaults.vibrantFloatingToolbarColors
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import killua.dev.arsenal.core.overlay.SimpleMenuItem
import killua.dev.arsenal.core.overlay.SimpleMenuMode
import killua.dev.arsenal.material.Spacing
import killua.dev.arsenal.material.components.ComponentContent
import killua.dev.arsenal.material.components.application.AppFilterChip
import killua.dev.arsenal.material.components.application.AppFilterChipsRow
import killua.dev.arsenal.material.components.application.AppPill
import killua.dev.arsenal.material.components.application.ArsenalSearchBar
import killua.dev.arsenal.material.components.application.SearchExpansionMode
import killua.dev.arsenal.material.components.application.appListItems
import killua.dev.arsenal.material.components.fabmenu.ArsenalFabMenu
import killua.dev.arsenal.material.components.menu.rememberSimpleMenuLauncher
import killua.dev.arsenal.material.components.fabmenu.FabMenuItemSpec
import killua.dev.arsenal.material.components.fabmenu.FabMenuSpec
import killua.dev.arsenal.material.overlay.LocalOverlayController
import killua.dev.arsenal.material.templates.basics.EmptyView
import killua.dev.arsenal.material.templates.basics.LoadingView
import killua.dev.arsenal.material.templates.basics.stateItem
import killua.dev.confundo.R
import killua.dev.confundo.ui.components.ExpressiveRefreshIndicator
import killua.dev.confundo.ui.components.TabScaffold
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeTab() {
    val viewModel: HomeViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val overlay = LocalOverlayController.current
    val scope = rememberCoroutineScope()
    val selecting = state.selectedPkgs.isNotEmpty()

    LaunchedEffect(Unit) { viewModel.dispatch(HomeIntent.Load) }
    BackHandler(selecting) { viewModel.dispatch(HomeIntent.ClearSelection) }

    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    val applyAllTitle = stringResource(R.string.dialog_apply_all_title)
    val applyAllMessage = stringResource(R.string.dialog_apply_all_message)
    val confirmLabel = stringResource(R.string.dialog_confirm)
    val cancelLabel = stringResource(R.string.dialog_cancel)

    TabScaffold(
        title = stringResource(R.string.app_name),
        actions = { FilterMenu(state, viewModel::dispatch) },
        floatingActionButton = {
            ArsenalFabMenu(
                spec = FabMenuSpec(
                    id = "home_fab",
                    items = listOf(
                        FabMenuItemSpec(
                            id = "apply_all",
                            content = ComponentContent(
                                label = stringResource(R.string.fab_apply_all),
                                icon = Icons.Filled.Apps,
                            ),
                            onClick = {
                                fabMenuExpanded = false
                                overlay?.dialog(
                                    title = applyAllTitle,
                                    message = applyAllMessage,
                                    confirmLabel = confirmLabel,
                                    dismissLabel = cancelLabel,
                                    onConfirm = { viewModel.dispatch(HomeIntent.ApplyToAll) },
                                )
                            },
                        ),
                        FabMenuItemSpec(
                            id = "template_manage",
                            content = ComponentContent(
                                label = stringResource(R.string.fab_template_manage),
                                icon = Icons.Filled.Menu,
                            ),
                            onClick = {
                                fabMenuExpanded = false
                                viewModel.dispatch(HomeIntent.OpenTemplateManage)
                            },
                        ),
                    ),
                ),
                expanded = fabMenuExpanded,
                button = {
                    ToggleFloatingActionButton(
                        checked = fabMenuExpanded,
                        onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
                        containerColor = ToggleFloatingActionButtonDefaults.containerColor(
                            initialColor = MaterialTheme.colorScheme.primaryContainer,
                            finalColor = MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    ) {
                        Icon(
                            if (fabMenuExpanded) Icons.Filled.Close else Icons.Filled.Menu,
                            stringResource(R.string.fab_template_manage),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            Column(Modifier.fillMaxSize()) {
                ArsenalSearchBar(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.dispatch(HomeIntent.SetSearchQuery(it)) },
                    expansionMode = SearchExpansionMode.Inline,
                    placeholder = stringResource(R.string.search_hint),
                    clearContentDescription = stringResource(R.string.cd_search_clear),
                    searchContentDescription = stringResource(R.string.cd_search),
                )
                AppFilterChipsRow(chips = statusChips(state.statusFilter) { viewModel.dispatch(HomeIntent.SetStatusFilter(it)) })

                when (state.phase) {
                    HomePhase.Loading -> LoadingView()
                    HomePhase.Ready -> {
                        val pullState = rememberPullToRefreshState()
                        PullToRefreshBox(
                            isRefreshing = state.isRefreshing,
                            onRefresh = { viewModel.dispatch(HomeIntent.Refresh) },
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
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, bottom = 96.dp),
                                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                            ) {
                                if (state.visibleApps.isEmpty()) {
                                    stateItem { fill ->
                                        EmptyView(
                                            icon = Icons.Filled.Apps,
                                            title = stringResource(R.string.search_empty),
                                            modifier = fill.animateItem(),
                                        )
                                    }
                                } else {
                                    appListItems(
                                        items = state.visibleApps,
                                        key = { it.packageName },
                                        packageName = { it.packageName },
                                        label = { it.appName },
                                        selected = { it.packageName in state.selectedPkgs },
                                        onClick = { app ->
                                            viewModel.dispatch(
                                                if (selecting) {
                                                    HomeIntent.ToggleSelect(app.packageName)
                                                } else {
                                                    HomeIntent.OpenAppDetail(app.packageName)
                                                },
                                            )
                                        },
                                        onLongClick = { app ->
                                            viewModel.dispatch(HomeIntent.ToggleSelect(app.packageName))
                                        },
                                        trailing = { app ->
                                            if (app.isSpoofingEnabled) {
                                                AppPill(text = stringResource(R.string.filter_enabled))
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = selecting,
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
                    .navigationBarsPadding(),
            ) {
                SelectionToolbar(
                    selectedCount = state.selectedPkgs.size,
                    allSelected = state.allVisibleSelected,
                    onExit = { viewModel.dispatch(HomeIntent.ClearSelection) },
                    onToggleSelectAll = { viewModel.dispatch(HomeIntent.ToggleSelectAllVisible) },
                    onOpenBatchActions = {
                        overlay?.bottomSheet { dismiss ->
                            var templates by remember { mutableStateOf<List<TemplateItem>?>(null) }
                            val pickerTemplates = templates
                            if (pickerTemplates == null) {
                                val batch: (HomeIntent) -> Unit = { intent ->
                                    dismiss()
                                    viewModel.dispatch(intent)
                                }
                                BatchActionsSheet(
                                    selectedCount = state.selectedPkgs.size,
                                    onJavaOn = { batch(HomeIntent.BatchSetEnabled(true)) },
                                    onJavaOff = { batch(HomeIntent.BatchSetEnabled(false)) },
                                    onNativeOn = { batch(HomeIntent.BatchSetNativeHook(true)) },
                                    onNativeOff = { batch(HomeIntent.BatchSetNativeHook(false)) },
                                    onResetOn = { batch(HomeIntent.BatchSetAutoReset(true)) },
                                    onResetOff = { batch(HomeIntent.BatchSetAutoReset(false)) },
                                    onApplyTemplate = {
                                        scope.launch {
                                            val loaded = viewModel.loadTemplates()
                                            if (loaded.isNotEmpty()) templates = loaded
                                        }
                                    },
                                )
                            } else {
                                TemplatePickerSheet(
                                    templates = pickerTemplates,
                                    onPick = { templateId ->
                                        dismiss()
                                        viewModel.dispatch(HomeIntent.ApplyTemplate(templateId))
                                    },
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun statusChips(filter: AppStatusFilter, onSelect: (AppStatusFilter) -> Unit): List<AppFilterChip> = listOf(
    AppFilterChip(
        label = stringResource(R.string.filter_all),
        selected = filter == AppStatusFilter.All,
        onClick = { onSelect(AppStatusFilter.All) },
    ),
    AppFilterChip(
        label = stringResource(R.string.filter_enabled),
        selected = filter == AppStatusFilter.Enabled,
        onClick = { onSelect(AppStatusFilter.Enabled) },
    ),
    AppFilterChip(
        label = stringResource(R.string.filter_disabled),
        selected = filter == AppStatusFilter.Disabled,
        onClick = { onSelect(AppStatusFilter.Disabled) },
    ),
)

private const val FILTER_SYSTEM_APPS_ID = "show_system_apps"

@Composable
private fun FilterMenu(state: HomeUiState, onIntent: (HomeIntent) -> Unit) {
    val launcher = rememberSimpleMenuLauncher()
    var anchor by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val systemAppsLabel = stringResource(R.string.menu_show_system_apps)
    val sortLabels = AppSortOrder.entries.map { stringResource(sortLabelRes(it)) }
    IconButton(
        onClick = {
            anchor?.let { coordinates ->
                launcher.show(
                    items = buildList {
                        add(
                            SimpleMenuItem(
                                id = FILTER_SYSTEM_APPS_ID,
                                label = systemAppsLabel,
                                trailingIcon = if (state.showSystemApps) {
                                    Icons.Rounded.CheckBox
                                } else {
                                    Icons.Rounded.CheckBoxOutlineBlank
                                },
                            ),
                        )
                        AppSortOrder.entries.forEachIndexed { index, order ->
                            add(
                                SimpleMenuItem(
                                    id = order.name,
                                    label = sortLabels[index],
                                    selected = order == state.sortOrder,
                                ),
                            )
                        }
                    },
                    anchor = coordinates,
                    mode = SimpleMenuMode.Navigation,
                    onItemClick = { id ->
                        if (id == FILTER_SYSTEM_APPS_ID) {
                            onIntent(HomeIntent.ToggleSystemApps)
                        } else {
                            AppSortOrder.entries.firstOrNull { it.name == id }
                                ?.let { onIntent(HomeIntent.SetSortOrder(it)) }
                        }
                    },
                )
            }
        },
        modifier = Modifier.onGloballyPositioned { anchor = it },
    ) {
        Icon(Icons.Rounded.FilterList, contentDescription = stringResource(R.string.cd_more_options))
    }
}

private fun sortLabelRes(order: AppSortOrder): Int = when (order) {
    AppSortOrder.NameAsc -> R.string.sort_name_asc
    AppSortOrder.NameDesc -> R.string.sort_name_desc
    AppSortOrder.EnabledFirst -> R.string.sort_enabled_first
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SelectionToolbar(
    selectedCount: Int,
    allSelected: Boolean,
    onExit: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onOpenBatchActions: () -> Unit,
) {
    HorizontalFloatingToolbar(
        modifier = Modifier.padding(bottom = 16.dp),
        expanded = true,
        colors = vibrantFloatingToolbarColors(),
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                FilledIconButton(
                    onClick = onExit,
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainerHighest),
                    ),
                ) {
                    Icon(Icons.Filled.Close, stringResource(R.string.toolbar_deselect_all))
                }
                FilledIconButton(
                    onClick = onToggleSelectAll,
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = contentColorFor(MaterialTheme.colorScheme.primaryContainer),
                    ),
                ) {
                    Icon(
                        if (allSelected) Icons.Filled.Deselect else Icons.Filled.SelectAll,
                        stringResource(R.string.toolbar_select_all),
                    )
                }
                Text(
                    text = stringResource(R.string.toolbar_selected_count, selectedCount),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                FilledTonalButton(
                    onClick = onOpenBatchActions,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = contentColorFor(MaterialTheme.colorScheme.tertiaryContainer),
                    ),
                ) {
                    Text(stringResource(R.string.toolbar_batch_actions))
                }
            }
        },
    )
}
