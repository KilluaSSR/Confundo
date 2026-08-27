package killua.dev.confundo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

// Material Expressive shape scale. Components reference ShapeRadius rather than raw dp values.
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(ShapeRadius.ExtraSmall),
    small = RoundedCornerShape(ShapeRadius.Small),
    medium = RoundedCornerShape(ShapeRadius.Medium),
    large = RoundedCornerShape(ShapeRadius.Large),
    extraLarge = RoundedCornerShape(ShapeRadius.ExtraLarge),
)
