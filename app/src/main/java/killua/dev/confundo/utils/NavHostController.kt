package killua.dev.confundo.utils

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController

val LocalNavController: ProvidableCompositionLocal<NavHostController?> =
    staticCompositionLocalOf { null }

val LocalDrawerController: ProvidableCompositionLocal<(() -> Unit)?> =
    staticCompositionLocalOf { null }