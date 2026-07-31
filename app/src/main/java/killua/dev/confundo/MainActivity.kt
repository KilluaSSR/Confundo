package killua.dev.confundo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import killua.dev.confundo.data.AppSettings
import killua.dev.confundo.navigation.AnimatedNavHost
import killua.dev.confundo.navigation.NavbarItems
import killua.dev.confundo.navigation.Routes
import killua.dev.confundo.navigation.TabEnterTransition
import killua.dev.confundo.navigation.TabExitTransition
import killua.dev.confundo.ui.pages.home.AppDetailPage
import killua.dev.confundo.ui.pages.home.HomePage
import killua.dev.confundo.ui.pages.home.SettingsIntent
import killua.dev.confundo.ui.pages.home.SettingsPage
import killua.dev.confundo.ui.pages.home.SettingsViewModel
import killua.dev.confundo.ui.pages.home.TemplateDetailPage
import killua.dev.confundo.ui.pages.home.TemplateManagePage
import killua.dev.confundo.ui.theme.ConfundoTheme
import killua.dev.confundo.ui.theme.Dimens
import killua.dev.confundo.ui.theme.ThemeMode
import killua.dev.confundo.utils.LocalNavController
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // 活动级 SettingsViewModel（以 Activity 为 ViewModelStoreOwner），
            // 用于把外观偏好响应式地驱动主题。
            val settingsVm: SettingsViewModel = hiltViewModel()
            val settingsState = settingsVm.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { settingsVm.emitIntentOnIO(SettingsIntent.Load) }

            // 仅派生主题所需的两个字段：其它设置项变化（如刷新间隔、上次运行时间）
            // 不会触发整棵树（主题 + 导航 + 当前页）重组。
            val themeMode by remember {
                derivedStateOf {
                    when (settingsState.value.darkMode) {
                        AppSettings.DARK_MODE_LIGHT -> ThemeMode.LIGHT
                        AppSettings.DARK_MODE_DARK -> ThemeMode.DARK
                        else -> ThemeMode.SYSTEM
                    }
                }
            }
            val dynamicColor by remember { derivedStateOf { settingsState.value.dynamicColor } }

            val rootNavController = rememberNavController()
            CompositionLocalProvider(
                LocalNavController provides rootNavController,
            ) {
                ConfundoTheme(
                    themeMode = themeMode,
                    dynamicColor = dynamicColor,
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
private fun MainScreen() {
    val navController = LocalNavController.current!!
    AnimatedNavHost(
        navController = navController,
        startDestination = Routes.MAIN_GRAPH,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Routes.MAIN_GRAPH) {
            MainTabsContainer()
        }

        composable(
            route = Routes.APP_DETAIL,
            arguments = listOf(navArgument("pkg") { type = NavType.StringType })
        ) { backStackEntry ->
            AppDetailPage(backStackEntry.arguments?.getString("pkg") ?: "")
        }

        composable(Routes.TEMPLATE_MANAGE) {
            TemplateManagePage()
        }

        composable(
            route = Routes.TEMPLATE_DETAIL,
            arguments = listOf(navArgument("templateId") { type = NavType.StringType })
        ) { backStackEntry ->
            TemplateDetailPage(backStackEntry.arguments?.getString("templateId") ?: "new")
        }
    }
}

@Composable
private fun MainTabsContainer() {
    val tabNavController = rememberNavController()
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 自适应导航：宽屏（平板/折叠屏展开/横屏）使用侧边 NavigationRail，紧凑宽度使用底部 NavigationBar。
    // 用 LocalConfiguration 判定断点，避免 BoxWithConstraints 的 SubcomposeLayout 包裹动画 NavHost 带来的额外开销。
    val useRail = LocalConfiguration.current.screenWidthDp.dp >= Dimens.CompactWidthBreakpoint

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail) {
                NavigationRail {
                    NavbarItems.items.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTab(tabNavController, item.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = stringResource(item.description)
                                )
                            },
                            label = { Text(stringResource(item.description)) },
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    AnimatedNavHost(
                        navController = tabNavController,
                        startDestination = Routes.HOME,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        // 同级 Tab 切换使用 Fade Through，而非 push 型横向滑动。
                        enterTransition = TabEnterTransition,
                        exitTransition = TabExitTransition,
                        popEnterTransition = TabEnterTransition,
                        popExitTransition = TabExitTransition,
                    ) {
                        composable(Routes.HOME) { HomePage() }
                        composable(Routes.SETTINGS) { SettingsPage() }
                    }

                    if (!useRail) {
                        NavigationBar {
                            NavbarItems.items.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTab(tabNavController, item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = stringResource(item.description)
                                        )
                                    },
                                    label = { Text(stringResource(item.description)) },
                                    alwaysShowLabel = true,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun navigateTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
