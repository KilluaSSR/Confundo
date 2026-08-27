package killua.dev.confundo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.shape.RoundedCornerShape
import killua.dev.confundo.ui.theme.ConfundoTheme
import killua.dev.confundo.ui.theme.ShapeRadius
import killua.dev.confundo.ui.theme.Spacing

enum class HighlightType {
    INFO,
    WARNING,
    CAUTION
}

@Composable
fun Highlight(
    warningType: HighlightType,
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    text: String
) {
    val backgroundColor = when (warningType) {
        HighlightType.WARNING -> MaterialTheme.colorScheme.primaryContainer
        HighlightType.INFO -> MaterialTheme.colorScheme.surfaceContainerHigh
        HighlightType.CAUTION -> MaterialTheme.colorScheme.errorContainer
    }

    val textColor = contentColorFor(backgroundColor)

    // 警示类高亮对无障碍声明为 live region，便于 TalkBack 主动播报安全提示。
    val a11yModifier = when (warningType) {
        HighlightType.WARNING, HighlightType.CAUTION ->
            Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        HighlightType.INFO -> Modifier
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(a11yModifier),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = textColor,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.none),
        shape = RoundedCornerShape(ShapeRadius.Large),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(Spacing.lgIncreased)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = title,
                    color = textColor,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun HighlightPreview() {
    ConfundoTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Highlight(
                warningType = HighlightType.INFO,
                icon = Icons.Default.CheckCircle,
                title = "1",
                text = "3，4。"
            )

            Highlight(
                warningType = HighlightType.WARNING,
                icon = Icons.Default.Warning,
                title = "2",
                text = "5，6，7。"
            )
        }
    }
}
