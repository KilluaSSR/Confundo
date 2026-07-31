package killua.dev.confundo.ui.theme

import androidx.compose.ui.unit.dp

// 统一的间距 / 尺寸 token，替代散落的魔法 dp。
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
}

object Dimens {
    /** 列表项最小触控高度（无障碍：≥ 48dp）。 */
    val MinTouchTarget = 48.dp

    /** 列表分组行圆角。 */
    val ListCorner = 20.dp

    /** 内容在大屏上的最大宽度，避免超长行影响可读性。 */
    val ContentMaxWidth = 640.dp

    /** 紧凑宽度断点，用于自适应导航（rail vs bar）。 */
    val CompactWidthBreakpoint = 600.dp
}
