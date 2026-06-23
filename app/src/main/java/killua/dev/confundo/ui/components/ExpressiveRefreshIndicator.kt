package killua.dev.confundo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveRefreshIndicator(
    isRefreshing: Boolean,
    state: PullToRefreshState,
    modifier: Modifier = Modifier,
) {
    val progress = if (isRefreshing) 1f else state.distanceFraction.coerceIn(0f, 1f)
    val scale by animateFloatAsState(targetValue = if (progress > 0f) 1f else 0.6f, label = "refreshScale")

    if (progress > 0f || isRefreshing) {
        ContainedLoadingIndicator(
            modifier = modifier
                .size(64.dp)
                .alpha(progress)
                .scale(scale),
        )
    }
}
