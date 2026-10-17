package killua.dev.confundo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import killua.dev.arsenal.material.colors.ArsenalTheme
import killua.dev.arsenal.material.colors.ColorSource
import killua.dev.confundo.R
import killua.dev.confundo.data.DarkMode

@Composable
fun ConfundoTheme(
    themeMode: DarkMode,
    dynamicColor: Boolean,
    transparentBackground: Boolean = false,
    content: @Composable () -> Unit,
) {
    val seed = colorResource(R.color.brand_seed)
    val darkTheme = when (themeMode) {
        DarkMode.System -> isSystemInDarkTheme()
        DarkMode.Light -> false
        DarkMode.Dark -> true
    }
    val source = if (dynamicColor) ColorSource.Wallpaper(fallbackSeed = seed) else ColorSource.Seed(seed = seed)
    ArsenalTheme(colorSource = source, darkTheme = darkTheme, content = content, transparentBackground = transparentBackground)
}
