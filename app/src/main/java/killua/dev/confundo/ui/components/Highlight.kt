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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

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

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = backgroundColor
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
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
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
