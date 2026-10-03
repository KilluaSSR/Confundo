package killua.dev.confundo.ui.pages.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import killua.dev.confundo.data.InjectMode
import killua.dev.confundo.data.SystemServerFields
import killua.dev.confundo.ui.components.AppDetailItem
import killua.dev.confundo.ui.components.AppPosition
import killua.dev.confundo.ui.components.CardSwitch
import killua.dev.confundo.ui.components.FieldInputDialog
import killua.dev.confundo.ui.components.Highlight
import killua.dev.confundo.ui.components.HighlightType
import killua.dev.confundo.ui.components.ObserveSnackbarEffects
import killua.dev.confundo.ui.components.PageLoadingIndicator
import killua.dev.confundo.ui.components.SectionHeader
import killua.dev.confundo.ui.components.animatedGroupedShape
import killua.dev.confundo.ui.theme.Dimens
import killua.dev.confundo.ui.theme.Spacing
import killua.dev.confundo.utils.LocalNavController

// 高风险应用：为其伪装指纹可能导致账号异常/封禁（见 README 特别提醒）。
private val HighRiskPackages = setOf(
    "com.tencent.mm",        // 微信
    "com.icbc",              // 工商银行
    "com.chinamworld.main",  // 中国银行
    "com.chinamworld.bocmbci", // 建设银行
    "cmb.pb",                // 招商银行
    "com.ecitic.bank.mobile", // 中信银行
    "com.android.bankabc",   // 农业银行
)

// Native Hook 风险应用：对其开启 Native Hook 可能导致闪退或功能异常。
private val NativeHookRiskPackages = setOf(
    "com.icbc",              // 工商银行
    "com.chinamworld.main",  // 中国银行
    "com.chinamworld.bocmbci", // 建设银行
    "cmb.pb",                // 招商银行
    "com.ecitic.bank.mobile", // 中信银行
    "com.android.bankabc",   // 农业银行
    "com.zhiliaoapp.musically", // TikTok
    "com.ss.android.ugc.trill", // TikTok
    "com.ss.android.ugc.aweme", // 抖音
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppDetailPage(pkg: String, viewModel: AppDetailViewModel = hiltViewModel()) {
    val navController = LocalNavController.current!!
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(pkg) { viewModel.emitIntentOnIO(AppDetailIntent.Load(pkg)) }

    val snackbarHostState = remember { SnackbarHostState() }
    ObserveSnackbarEffects(viewModel.effects, snackbarHostState)

    var editingSpec by remember { mutableStateOf<FieldSpec?>(null) }
    var editingTitle by remember { mutableStateOf("") }
    var editingValue by remember { mutableStateOf("") }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.appName.ifEmpty { pkg },
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
                        onClick = { viewModel.emitIntentOnIO(AppDetailIntent.RandomFill) },
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

                        if (pkg in HighRiskPackages) {
                            Highlight(
                                warningType = HighlightType.CAUTION,
                                icon = Icons.Filled.Warning,
                                title = stringResource(R.string.warning_high_risk_title),
                                text = stringResource(R.string.warning_high_risk_message),
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        CardSwitch(
                            text = stringResource(R.string.switch_enable),
                            checked = state.enabled,
                            onCheckedChange = { viewModel.emitIntentOnIO(AppDetailIntent.SetEnabled(it)) },
                            modifier = Modifier.padding(horizontal = 16.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                            shape = animatedGroupedShape(AppPosition.Top, false, Dimens.ListCorner),
                        )

                        Spacer(modifier = Modifier.height(Spacing.xxs))

                        CardSwitch(
                            text = stringResource(R.string.switch_auto_reset),
                            checked = state.autoReset,
                            onCheckedChange = if (state.enabled) {
                                { viewModel.emitIntentOnIO(AppDetailIntent.SetAutoReset(it)) }
                            } else null,
                            enabled = state.enabled,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                            shape = animatedGroupedShape(AppPosition.Bottom, false, Dimens.ListCorner),
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        InjectModeCard(
                            mode = state.injectMode,
                            enabled = state.enabled,
                            onModeChange = { viewModel.emitIntentOnIO(AppDetailIntent.SetInjectMode(it)) },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )

                        AnimatedVisibility(
                            visible = state.nativeHookGlobalEnabled &&
                                state.enabled &&
                                state.injectMode == InjectMode.IN_APP,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))

                                if (pkg in NativeHookRiskPackages) {
                                    Highlight(
                                        warningType = HighlightType.CAUTION,
                                        icon = Icons.Filled.Warning,
                                        title = stringResource(R.string.warning_native_hook_risk_title),
                                        text = stringResource(R.string.warning_native_hook_risk_message),
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                CardSwitch(
                                    text = stringResource(R.string.switch_native_hook),
                                    checked = state.nativeHookEnabled,
                                    onCheckedChange = {
                                        viewModel.emitIntentOnIO(AppDetailIntent.SetNativeHook(it))
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                                    shape = animatedGroupedShape(AppPosition.Single, false, Dimens.ListCorner),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        AnimatedContent(
                            targetState = state.injectMode,
                            label = "inject-mode-fields",
                        ) { mode ->
                            Column {
                                FieldCatalog.grouped.forEach { (category, allSpecs) ->
                                    val specs = if (mode == InjectMode.SYSTEM_SERVER) {
                                        allSpecs.filter { SystemServerFields.supports(it.key) }
                                    } else {
                                        allSpecs
                                    }
                                    if (specs.isEmpty()) return@forEach

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
                                                    enabled = state.enabled,
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
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    FieldInputDialog(
        spec = editingSpec,
        title = editingTitle,
        initialValue = editingValue,
        onConfirm = { value ->
            editingSpec?.let { viewModel.emitIntentOnIO(AppDetailIntent.UpdateField(it.key, value)) }
            editingSpec = null
        },
        onDismiss = { editingSpec = null }
    )
}

@Composable
private fun InjectModeCard(
    mode: InjectMode,
    enabled: Boolean,
    onModeChange: (InjectMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = animatedGroupedShape(AppPosition.Single, false, Dimens.ListCorner),
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(
                text = stringResource(R.string.inject_mode_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(Spacing.sm))
            val modes = InjectMode.entries
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                modes.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = mode == option,
                        onClick = { onModeChange(option) },
                        enabled = enabled,
                        shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                    ) {
                        Text(stringResource(option.labelRes))
                    }
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            AnimatedContent(targetState = mode, label = "inject-mode-summary") { current ->
                Text(
                    text = stringResource(current.summaryRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
