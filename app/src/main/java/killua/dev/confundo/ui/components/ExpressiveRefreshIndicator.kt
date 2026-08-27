package killua.dev.confundo.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import killua.dev.confundo.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveRefreshIndicator(
    isRefreshing: Boolean,
    state: PullToRefreshState,
    modifier: Modifier = Modifier,
) {
    ContainedLoadingIndicator(
        modifier = modifier
            .padding(top = Spacing.sm)
            .graphicsLayer {
                val progress =
                    if (isRefreshing) 1f else state.distanceFraction.coerceIn(0f, 1f)
                val scale = 0.6f + 0.4f * progress
                alpha = progress
                scaleX = scale
                scaleY = scale
            },
    )
}
