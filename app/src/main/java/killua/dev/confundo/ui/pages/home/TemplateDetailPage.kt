package killua.dev.confundo.ui.pages.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import killua.dev.confundo.R
import killua.dev.confundo.data.FieldCatalog
import killua.dev.confundo.data.FieldSpec
import killua.dev.confundo.ui.components.AppDetailItem
import killua.dev.confundo.ui.components.AppPosition
import killua.dev.confundo.ui.components.FieldInputDialog
import killua.dev.confundo.ui.components.ObserveSnackbarEffects
import killua.dev.confundo.ui.components.PageLoadingIndicator
import killua.dev.confundo.ui.components.SectionHeader
import killua.dev.confundo.ui.components.TextInputDialog
import killua.dev.confundo.ui.components.animatedGroupedShape
import killua.dev.confundo.ui.theme.Dimens
import killua.dev.confundo.ui.theme.ShapeRadius
import killua.dev.confundo.ui.theme.Spacing
import killua.dev.confundo.utils.LocalNavController

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TemplateDetailPage(
    templateId: String,
    viewModel: TemplateDetailViewModel = hiltViewModel()
) {
    val navController = LocalNavController.current!!
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(templateId) { viewModel.emitIntentOnIO(TemplateDetailIntent.Load(templateId)) }

    val snackbarHostState = remember { SnackbarHostState() }
    ObserveSnackbarEffects(viewModel.effects, snackbarHostState)

    var editingSpec by remember { mutableStateOf<FieldSpec?>(null) }
    var editingTitle by remember { mutableStateOf("") }
    var editingValue by remember { mutableStateOf("") }
    var editingName by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        state.name.ifEmpty {
                            if (state.isNew) stringResource(R.string.template_new) else ""
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
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
                actions = {
                    IconButton(
                        onClick = { viewModel.emitIntentOnIO(TemplateDetailIntent.RandomFill) },
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.menu_random_fill)
                        )
                    }
                },
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
                            .widthIn(max = Dimens.ContentMaxWidth)
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                            )
                            .align(Alignment.TopCenter)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
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
                            SectionHeader(title = stringResource(category.titleRes))
                            Column(
                                modifier = Modifier.padding(horizontal = Spacing.lg),
                                verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
                            ) {
                                specs.forEachIndexed { index, spec ->
                                    val title = stringResource(spec.labelRes)
                                    val position = when {
                                        specs.size == 1 -> AppPosition.Single
                                        index == 0 -> AppPosition.Top
                                        index == specs.lastIndex -> AppPosition.Bottom
                                        else -> AppPosition.Middle
                                    }
                                    Surface(
                                        shape = animatedGroupedShape(position, false, Dimens.ListCorner),
                                        color = MaterialTheme.colorScheme.surfaceBright,
                                    ) {
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
