package killua.dev.confundo.ui.pages.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import killua.dev.confundo.R
import killua.dev.confundo.ui.components.AppItemData
import killua.dev.confundo.ui.components.AppListRow
import killua.dev.confundo.ui.components.AppPosition
import killua.dev.confundo.ui.components.ObserveSnackbarEffects
import killua.dev.confundo.ui.components.PageLoadingIndicator
import killua.dev.confundo.ui.components.animatedGroupedShape
import killua.dev.confundo.ui.theme.Dimens
import killua.dev.confundo.utils.LocalNavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CopySourcePage(
    targetPkg: String,
    viewModel: CopySourceViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current!!
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(targetPkg) { viewModel.emitIntentOnIO(CopySourceIntent.Load(targetPkg)) }

    val snackbarHostState = remember { SnackbarHostState() }
    ObserveSnackbarEffects(viewModel.effects, snackbarHostState)

    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { viewModel.emitIntentOnIO(CopySourceIntent.SetSearchQuery(it)) }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val applyAndClose: (String) -> Unit = { pkg ->
        viewModel.emitIntentOnIO(CopySourceIntent.Apply(pkg))
        navController.popBackStack()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.template_pick_title)) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        ) {
            when (state.phase) {
                HomePhase.Loading -> PageLoadingIndicator()
                HomePhase.Ready -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = Dimens.ContentMaxWidth)
                            .align(Alignment.TopCenter),
                    ) {
                        CopySearchBar(
                            textFieldState = textFieldState,
                            searchBarState = searchBarState,
                            results = state.visibleSources,
                            icons = state.icons,
                            onResultClick = applyAndClose,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .widthIn(max = Dimens.ContentMaxWidth)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )

                        val sources = state.visibleSources
                        if (sources.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 64.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.template_pick_empty),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                itemsIndexed(
                                    sources,
                                    key = { _, app -> app.packageName },
                                ) { index, app ->
                                    val position = when {
                                        sources.size <= 1 -> AppPosition.Single
                                        index == 0 -> AppPosition.Top
                                        index == sources.lastIndex -> AppPosition.Bottom
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
                                            isSpoofingEnabled = false,
                                        ),
                                        position = position,
                                        onClick = { applyAndClose(app.packageName) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CopySearchBar(
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
        CopySearchResults(
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
private fun CopySearchResults(
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
                text = stringResource(R.string.template_pick_empty),
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
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                        ),
                        modifier = Modifier.clickable { onResultClick(app.packageName) },
                    )
                }
            }
        }
    }
}
