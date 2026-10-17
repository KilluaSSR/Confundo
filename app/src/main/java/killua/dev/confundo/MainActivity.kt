package killua.dev.confundo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import killua.dev.arsenal.core.overlay.OverlayController
import killua.dev.arsenal.core.transition.ArsenalSharedTransitionLayout
import killua.dev.arsenal.data.settings.SettingsStore
import killua.dev.arsenal.material.overlay.ArsenalOverlayHost
import killua.dev.arsenal.navigation.AnimatedNavHost
import killua.dev.arsenal.navigation.NavigationCommandHandler
import killua.dev.arsenal.navigation.Navigator
import killua.dev.arsenal.navigation.generated.GeneratedRoutes
import killua.dev.arsenal.navigation.generated.arsenalGeneratedDestinations
import killua.dev.confundo.data.ConfundoSettings
import killua.dev.confundo.ui.theme.ConfundoTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var overlayController: OverlayController

    @Inject
    lateinit var settingsStore: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkMode by settingsStore.state(ConfundoSettings.darkMode)
                .collectAsStateWithLifecycle()
            val dynamicColor by settingsStore.state(ConfundoSettings.dynamicColor)
                .collectAsStateWithLifecycle()

            ConfundoTheme(themeMode = darkMode, dynamicColor = dynamicColor) {
                val navController = rememberNavController()
                NavigationCommandHandler(navigator = navigator, navController = navController)
                ArsenalOverlayHost(controller = overlayController) {
                    ArsenalSharedTransitionLayout {
                        AnimatedNavHost(
                            navController = navController,
                            startDestination = GeneratedRoutes.START_DESTINATION,
                        ) {
                            arsenalGeneratedDestinations(navController)
                        }
                    }
                }
            }
        }
    }
}
