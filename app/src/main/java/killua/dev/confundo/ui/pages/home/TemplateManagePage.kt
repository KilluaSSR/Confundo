package killua.dev.confundo.ui.pages.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.SelectAll
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import killua.dev.confundo.R
import killua.dev.confundo.navigation.Routes
import killua.dev.confundo.ui.components.AppPosition
import killua.dev.confundo.ui.components.PageLoadingIndicator
import killua.dev.confundo.ui.components.TemplateListRow
import killua.dev.confundo.utils.LocalNavController

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TemplateManagePage(viewModel: TemplateManageViewModel = hiltViewModel()) {
    val navController = LocalNavController.current!!
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.emitIntentOnIO(TemplateManageIntent.Load) }

    val selectedIds = remember { mutableStateListOf<String>() }
    val showToolbar = selectedIds.isNotEmpty()

    BackHandler(showToolbar) { selectedIds.clear() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.template_manage)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(Routes.templateDetail("new"))
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state.phase) {
                TemplateManagePhase.Loading -> PageLoadingIndicator()
                TemplateManagePhase.Ready -> {
                    if (state.templates.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.template_no_templates),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(
                                top = 8.dp,
                                bottom = 88.dp
                            )
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
                        Row(
                            Modifier
                                .wrapContentWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilledIconButton(onClick = { selectedIds.clear() }) {
                                Icon(
                                    Icons.Default.Deselect,
                                    stringResource(R.string.toolbar_deselect_all)
                                )
                            }
                            FilledIconButton(onClick = {
                                selectedIds.clear()
                                selectedIds.addAll(state.templates.map { it.id })
                            }) {
                                Icon(
                                    Icons.Default.SelectAll,
                                    stringResource(R.string.toolbar_select_all)
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    val ids = selectedIds.toList()
                                    selectedIds.clear()
                                    viewModel.emitIntentOnIO(TemplateManageIntent.DeleteSelected(ids))
                                },
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
}
