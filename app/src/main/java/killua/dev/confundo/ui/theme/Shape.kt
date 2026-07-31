package killua.dev.confundo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// 统一的形状体系（shape scale），严格对齐 MD3 shape token，避免散落的魔法圆角。
// 需要更强的 Expressive 圆角时，由具体容器显式选用更大的 token（如分组列表用
// large-increased≈20dp、分区卡片用 extraLarge=28dp），而不是整体上移基线刻度。
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
