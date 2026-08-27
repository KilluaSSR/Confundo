package killua.dev.confundo.ui.theme

import androidx.compose.ui.unit.dp

// 统一的间距 / 尺寸 token，替代散落的魔法 dp。
object Spacing {
    val none = 0.dp
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val lgIncreased = 20.dp
    val xl = 24.dp
    val xxl = 28.dp
    val xxxl = 32.dp
}

/** Material Expressive shape scale. */
object ShapeRadius {
    val None = 0.dp
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val ExtraLarge = 28.dp
    val Full = 999.dp
}

/** Material tonal/shadow elevation scale. Prefer tonal surfaces for non-floating content. */
object ShadowElevation {
    val level1 = 1.dp
    val level2 = 3.dp
    val level3 = 6.dp
    val level4 = 8.dp
    val level5 = 12.dp
}

object Dimens {
    /** 列表项最小触控高度（无障碍：≥ 48dp）。 */
    val MinTouchTarget = 48.dp

    /** 列表分组行圆角。 */
    val ListCorner = ShapeRadius.Large

    /** 内容在大屏上的最大宽度，避免超长行影响可读性。 */
    val ContentMaxWidth = 640.dp

    /** 紧凑宽度断点，用于自适应导航（rail vs bar）。 */
    val CompactWidthBreakpoint = 600.dp
}
