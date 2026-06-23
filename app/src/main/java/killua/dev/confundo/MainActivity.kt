package killua.dev.confundo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import killua.dev.confundo.navigation.AnimatedNavHost
import killua.dev.confundo.navigation.NavbarItems
import killua.dev.confundo.navigation.Routes
import killua.dev.confundo.ui.pages.home.AppDetailPage
import killua.dev.confundo.ui.pages.home.HomePage
import killua.dev.confundo.ui.pages.home.SettingsPage
import killua.dev.confundo.ui.pages.home.TemplateDetailPage
import killua.dev.confundo.ui.pages.home.TemplateManagePage
import killua.dev.confundo.ui.theme.ConfundoTheme
import killua.dev.confundo.utils.LocalNavController

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val rootNavController = rememberNavController()
            CompositionLocalProvider(
                LocalNavController provides rootNavController,
            ) {
                ConfundoTheme {
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

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedNavHost(
            navController = tabNavController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            composable(Routes.HOME) {
                HomePage()
            }
            composable(Routes.SETTINGS) {
                SettingsPage()
            }
        }
        NavigationBar {
            NavbarItems.items.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        tabNavController.navigate(item.route) {
                            popUpTo(tabNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
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
