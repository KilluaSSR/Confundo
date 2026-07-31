package killua.dev.confundo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.padding
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
    // 随下拉距离淡入并轻微放大，符合 Material Motion 的渐进反馈。
    val scale by animateFloatAsState(
        targetValue = 0.6f + 0.4f * progress,
        label = "refreshScale",
    )

    if (progress > 0f || isRefreshing) {
        // 使用默认尺寸的容器指示器，不再强制 size 造成容器与内部形状比例失衡。
        ContainedLoadingIndicator(
            modifier = modifier
                .padding(top = 8.dp)
                .alpha(progress)
                .scale(scale),
        )
    }
}
