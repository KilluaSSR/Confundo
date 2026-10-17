package killua.dev.confundo.ui.pages.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import killua.dev.arsenal.material.Spacing
import killua.dev.arsenal.material.components.application.ArsenalSearchBar
import killua.dev.arsenal.material.components.application.SearchExpansionMode
import killua.dev.arsenal.material.components.application.appListItems
import killua.dev.arsenal.material.templates.basics.EmptyView
import killua.dev.arsenal.material.templates.basics.LoadingView
import killua.dev.arsenal.navigation.annotation.NavDestination
import killua.dev.confundo.R
import killua.dev.confundo.ui.components.DetailScaffold

@OptIn(ExperimentalMaterial3Api::class)
@NavDestination(route = "copy_source/{pkg}")
@Composable
fun CopySourcePage(backStackEntry: NavBackStackEntry) {
    val viewModel: CopySourceViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val targetPkg = backStackEntry.arguments?.getString("pkg").orEmpty()

    LaunchedEffect(targetPkg) { viewModel.dispatch(CopySourceIntent.Load(targetPkg)) }

    DetailScaffold(
        title = stringResource(R.string.template_pick_title),
        onBack = { viewModel.dispatch(CopySourceIntent.Back) },
    ) { padding ->
        when (state.phase) {
            HomePhase.Loading -> LoadingView(modifier = Modifier.padding(padding))
            HomePhase.Ready -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    ArsenalSearchBar(
                        query = state.searchQuery,
                        onQueryChange = { viewModel.dispatch(CopySourceIntent.SetSearchQuery(it)) },
                        placeholder = stringResource(R.string.search_hint),
                        expansionMode = SearchExpansionMode.Inline,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val sources = state.visibleSources
                    if (sources.isEmpty()) {
                        EmptyView(
                            icon = Icons.Rounded.Apps,
                            title = stringResource(R.string.template_pick_empty),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = Spacing.md,
                                end = Spacing.md,
                                top = Spacing.sm,
                                bottom = Spacing.lg,
                            ),
                        ) {
                            appListItems(
                                items = sources,
                                key = { it.packageName },
                                packageName = { it.packageName },
                                label = { it.appName },
                                onClick = {
                                    viewModel.dispatch(CopySourceIntent.Apply(it.packageName))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
