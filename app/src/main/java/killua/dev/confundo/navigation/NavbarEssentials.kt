package killua.dev.confundo.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import killua.dev.confundo.R

data class NavBarItem(
    @StringRes val description: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
)

object NavbarItems {
    val items = listOf(
        NavBarItem(
            description = R.string.nav_home,
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            route = Routes.HOME
        ),
        NavBarItem(
            description = R.string.nav_settings,
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            route = Routes.SETTINGS
        )
    )
}
