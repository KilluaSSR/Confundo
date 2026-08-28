package killua.dev.confundo.ui.pages.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.FloatingToolbarDefaults.vibrantFloatingToolbarColors
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import killua.dev.confundo.R
import killua.dev.confundo.navigation.Routes
import killua.dev.confundo.ui.components.AppItemData
import killua.dev.confundo.ui.components.AppListRow
import killua.dev.confundo.ui.components.AppPosition
import killua.dev.confundo.ui.components.ExpressiveRefreshIndicator
import killua.dev.confundo.ui.components.ObserveSnackbarEffects
import killua.dev.confundo.ui.components.PageLoadingIndicator
import killua.dev.confundo.ui.components.animatedGroupedShape
import killua.dev.confundo.ui.theme.Dimens
import killua.dev.confundo.ui.theme.ShapeRadius
import killua.dev.confundo.utils.LocalNavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomePage(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = LocalNavController.current!!

    LaunchedEffect(Unit) { viewModel.emitIntentOnIO(HomeIntent.Load) }

    val snackbarHostState = remember { SnackbarHostState() }
    ObserveSnackbarEffects(viewModel.effects, snackbarHostState)

    val selectedPkgs = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() },
        )
    ) { mutableStateListOf<String>() }
    val showToolbar = selectedPkgs.isNotEmpty()

    BackHandler(showToolbar) { selectedPkgs.clear() }

    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    var showMenu by remember { mutableStateOf(false) }

    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { viewModel.emitIntentOnIO(HomeIntent.SetSearchQuery(it)) }
    }

    val visibleApps = state.visibleApps

    val scope = rememberCoroutineScope()

    var showApplyAllDialog by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }
    var templatePickerItems by remember { mutableStateOf<List<TemplateItem>>(emptyList()) }
    var showBatchSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                actions = {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            shapes = IconButtonDefaults.shapes(),
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.cd_more_options)
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
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                )
        ) {
            when (state.phase) {
                HomePhase.Loading -> PageLoadingIndicator()
                HomePhase.Ready -> {
                    Column(Modifier.fillMaxSize()) {
                        HomeSearchBar(
                            textFieldState = textFieldState,
                            searchBarState = searchBarState,
                            results = visibleApps,
                            icons = state.icons,
                            onResultClick = { pkg ->
                                navController.navigate(Routes.appDetail(pkg))
                            },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .widthIn(max = Dimens.ContentMaxWidth)
                                .fillMaxWidth()
                                .windowInsetsPadding(
                                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )

                        FilterSortRow(
                            statusFilter = state.statusFilter,
                            sortOrder = state.sortOrder,
                            onStatusFilter = {
                                viewModel.emitIntentOnIO(HomeIntent.SetStatusFilter(it))
                            },
                            onSortOrder = {
                                viewModel.emitIntentOnIO(HomeIntent.SetSortOrder(it))
                            },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .widthIn(max = Dimens.ContentMaxWidth)
                                .fillMaxWidth()
                                .windowInsetsPadding(
                                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                                )
                                .padding(horizontal = 16.dp),
                        )

                        val pullState = rememberPullToRefreshState()
                        PullToRefreshBox(
                            isRefreshing = state.isRefreshing,
                            onRefresh = { viewModel.emitIntentOnIO(HomeIntent.Refresh) },
                            state = pullState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
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
                                    .fillMaxHeight()
                                    .widthIn(max = Dimens.ContentMaxWidth)
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .windowInsetsPadding(
                                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                                    )
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                if (visibleApps.isEmpty()) {
                                    item(key = "__empty__") {
                                        EmptyResult(modifier = Modifier.animateItem())
                                    }
                                } else {
                                    itemsIndexed(
                                        visibleApps,
                                        key = { _, app -> app.packageName }
                                    ) { index, app ->
                                        val position = when {
                                            visibleApps.size <= 1 -> AppPosition.Single
                                            index == 0 -> AppPosition.Top
                                            index == visibleApps.lastIndex -> AppPosition.Bottom
                                            else -> AppPosition.Middle
                                        }
                                        AppListRow(
                                            modifier = Modifier.animateItem(),
                                            appData = AppItemData(
                                                id = app.packageName,
                                                icon = Icons.Rounded.Android,
                                                iconBitmap = state.icons[app.packageName],
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
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showToolbar,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    )
                ) { it } + fadeIn(
                    spring(stiffness = Spring.StiffnessMedium)
                ),
                exit = slideOutVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium,
                    )
                ) { it } + fadeOut(
                    spring(stiffness = Spring.StiffnessMedium)
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                SelectionToolbar(
                    selectedCount = selectedPkgs.size,
                    allSelected = visibleApps.isNotEmpty() &&
                        visibleApps.all { it.packageName in selectedPkgs },
                    onExit = { selectedPkgs.clear() },
                    onToggleSelectAll = {
                        val all = visibleApps.map { it.packageName }
                        if (all.isNotEmpty() && all.all { it in selectedPkgs }) {
                            selectedPkgs.clear()
                        } else {
                            selectedPkgs.clear()
                            selectedPkgs.addAll(all)
                        }
                    },
                    onOpenBatchActions = { showBatchSheet = true },
                )
            }

            FloatingActionButtonMenu(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp),
                expanded = fabMenuExpanded,
                button = {
                    ToggleFloatingActionButton(
                        checked = fabMenuExpanded,
                        onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
                        containerColor = ToggleFloatingActionButtonDefaults.containerColor(
                            initialColor = MaterialTheme.colorScheme.primaryContainer,
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
            title = { Text(stringResource(R.string.dialog_apply_all_title)) },
            text = { Text(stringResource(R.string.dialog_apply_all_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showApplyAllDialog = false
                        viewModel.emitIntentOnIO(HomeIntent.ApplyToAll)
                    },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showApplyAllDialog = false },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    if (showTemplatePicker) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showTemplatePicker = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(
                topStart = ShapeRadius.ExtraLarge,
                topEnd = ShapeRadius.ExtraLarge,
            ),
            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f),
        ) {
            Text(
                text = stringResource(R.string.template_apply_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                templatePickerItems.forEachIndexed { index, template ->
                    val position = when {
                        templatePickerItems.size == 1 -> AppPosition.Single
                        index == 0 -> AppPosition.Top
                        index == templatePickerItems.lastIndex -> AppPosition.Bottom
                        else -> AppPosition.Middle
                    }
                    Surface(
                        shape = animatedGroupedShape(position, false, Dimens.ListCorner),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        ListItem(
                            headlineContent = { Text(template.name) },
                            colors = androidx.compose.material3.ListItemDefaults.colors(
                                containerColor = androidx.compose.ui.graphics.Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showTemplatePicker = false
                                    val pkgs = selectedPkgs.toList()
                                    selectedPkgs.clear()
                                    viewModel.emitIntentOnIO(
                                        HomeIntent.ApplyTemplate(pkgs, template.id)
                                    )
                                },
                        )
                    }
                }
            }
            Box(modifier = Modifier.navigationBarsPadding())
        }
    }

    if (showBatchSheet) {
        val sheetState = rememberModalBottomSheetState()
        val runBatch: (HomeIntent) -> Unit = { intent ->
            showBatchSheet = false
            selectedPkgs.clear()
            viewModel.emitIntentOnIO(intent)
        }
        ModalBottomSheet(
            onDismissRequest = { showBatchSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(
                topStart = ShapeRadius.ExtraLarge,
                topEnd = ShapeRadius.ExtraLarge,
            ),
            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f),
        ) {
            BatchActionsSheet(
                selectedCount = selectedPkgs.size,
                onJavaOn = { runBatch(HomeIntent.BatchSetEnabled(selectedPkgs.toList(), true)) },
                onJavaOff = { runBatch(HomeIntent.BatchSetEnabled(selectedPkgs.toList(), false)) },
                onNativeOn = { runBatch(HomeIntent.BatchSetNativeHook(selectedPkgs.toList(), true)) },
                onNativeOff = { runBatch(HomeIntent.BatchSetNativeHook(selectedPkgs.toList(), false)) },
                onResetOn = { runBatch(HomeIntent.BatchSetAutoReset(selectedPkgs.toList(), true)) },
                onResetOff = { runBatch(HomeIntent.BatchSetAutoReset(selectedPkgs.toList(), false)) },
                onApplyTemplate = {
                    showBatchSheet = false
                    scope.launch {
                        val templates = viewModel.loadTemplates()
                        if (templates.isNotEmpty()) {
                            templatePickerItems = templates
                            showTemplatePicker = true
                        }
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeSearchBar(
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    results: List<AppListItem>,
    icons: Map<String, ImageBitmap?>,
    onResultClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    val inputField = @Composable {
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = stringResource(R.string.cd_search),
                )
            },
            trailingIcon = {
                if (textFieldState.text.isNotEmpty()) {
                    IconButton(
                        onClick = { textFieldState.clearText() },
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.cd_search_clear),
                        )
                    }
                }
            },
        )
    }

    SearchBar(
        state = searchBarState,
        inputField = inputField,
        modifier = modifier,
    )

    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField,
    ) {
        SearchResults(
            results = results,
            icons = icons,
            onResultClick = { pkg ->
                scope.launch { searchBarState.animateToCollapsed() }
                onResultClick(pkg)
            },
        )
    }
}

@Composable
private fun SearchResults(
    results: List<AppListItem>,
    icons: Map<String, ImageBitmap?>,
    onResultClick: (String) -> Unit,
) {
    if (results.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.search_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(results, key = { _, app -> app.packageName }) { index, app ->
                val position = when {
                    results.size == 1 -> AppPosition.Single
                    index == 0 -> AppPosition.Top
                    index == results.lastIndex -> AppPosition.Bottom
                    else -> AppPosition.Middle
                }
                Surface(
                    shape = animatedGroupedShape(position, false, Dimens.ListCorner),
                    color = MaterialTheme.colorScheme.surfaceBright,
                ) {
                    ListItem(
                        headlineContent = { Text(app.appName) },
                        supportingContent = {
                            Text(
                                app.packageName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingContent = {
                            val bitmap = icons[app.packageName]
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                )
                            } else {
                                Icon(Icons.Rounded.Android, contentDescription = null)
                            }
                        },
                        colors = androidx.compose.material3.ListItemDefaults.colors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        modifier = Modifier.clickable { onResultClick(app.packageName) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSortRow(
    statusFilter: AppStatusFilter,
    sortOrder: AppSortOrder,
    onStatusFilter: (AppStatusFilter) -> Unit,
    onSortOrder: (AppSortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val statuses = listOf(
            AppStatusFilter.All to R.string.filter_all,
            AppStatusFilter.Enabled to R.string.filter_enabled,
            AppStatusFilter.Disabled to R.string.filter_disabled,
        )
        statuses.forEach { (status, labelRes) ->
            FilterChip(
                selected = statusFilter == status,
                onClick = { onStatusFilter(status) },
                label = { Text(stringResource(labelRes)) },
            )
        }

        Box(modifier = Modifier.weight(1f))

        var sortMenu by remember { mutableStateOf(false) }
        Box {
            AssistChip(
                onClick = { sortMenu = true },
                label = { Text(stringResource(sortLabelRes(sortOrder))) },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Filled.Sort,
                        contentDescription = stringResource(R.string.cd_sort),
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                val orders = listOf(
                    AppSortOrder.NameAsc,
                    AppSortOrder.NameDesc,
                    AppSortOrder.EnabledFirst,
                )
                orders.forEach { order ->
                    DropdownMenuItem(
                        text = { Text(stringResource(sortLabelRes(order))) },
                        onClick = {
                            sortMenu = false
                            onSortOrder(order)
                        },
                        leadingIcon = if (order == sortOrder) {
                            { Icon(Icons.Filled.Check, contentDescription = null) }
                        } else null,
                    )
                }
            }
        }
    }
}

private fun sortLabelRes(order: AppSortOrder): Int = when (order) {
    AppSortOrder.NameAsc -> R.string.sort_name_asc
    AppSortOrder.NameDesc -> R.string.sort_name_desc
    AppSortOrder.EnabledFirst -> R.string.sort_enabled_first
}

@Composable
private fun EmptyResult(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.search_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
                    )
                ) {
                    Icon(Icons.Filled.Close, stringResource(R.string.toolbar_deselect_all))
                }
                FilledIconButton(
                    onClick = onToggleSelectAll,
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = contentColorFor(MaterialTheme.colorScheme.primaryContainer),
                    )
                ) {
                    Icon(
                        if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
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
                    )
                ) {
                    Text(stringResource(R.string.toolbar_batch_actions))
                }
            }
        },
    )
}

@Composable
private fun BatchActionsSheet(
    selectedCount: Int,
    onJavaOn: () -> Unit,
    onJavaOff: () -> Unit,
    onNativeOn: () -> Unit,
    onNativeOff: () -> Unit,
    onResetOn: () -> Unit,
    onResetOff: () -> Unit,
    onApplyTemplate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.batch_sheet_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.toolbar_selected_count, selectedCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        BatchToggleRow(
            label = stringResource(R.string.batch_group_java),
            hint = null,
            onOn = onJavaOn,
            onOff = onJavaOff,
        )
        BatchToggleRow(
            label = stringResource(R.string.batch_group_native),
            hint = stringResource(R.string.batch_native_hint),
            onOn = onNativeOn,
            onOff = onNativeOff,
        )
        BatchToggleRow(
            label = stringResource(R.string.batch_group_reset),
            hint = null,
            onOn = onResetOn,
            onOff = onResetOff,
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.batch_group_template),
                style = MaterialTheme.typography.titleSmall,
            )
            FilledTonalButton(
                onClick = onApplyTemplate,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = contentColorFor(MaterialTheme.colorScheme.primaryContainer),
                ),
            ) {
                Text(stringResource(R.string.toolbar_apply_template))
            }
        }

        Box(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun BatchToggleRow(
    label: String,
    hint: String?,
    onOn: () -> Unit,
    onOff: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = onOn,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = contentColorFor(MaterialTheme.colorScheme.tertiaryContainer),
                ),
            ) {
                Text(stringResource(R.string.batch_on))
            }
            FilledTonalButton(
                onClick = onOff,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = contentColorFor(MaterialTheme.colorScheme.secondaryContainer),
                ),
            ) {
                Text(stringResource(R.string.batch_off))
            }
        }
    }
}
