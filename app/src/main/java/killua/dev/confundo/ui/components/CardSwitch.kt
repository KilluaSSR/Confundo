package killua.dev.confundo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CardSwitch(
    text: String,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    enabled: Boolean = true,
) {
    val contentAlpha = if (enabled) 1f else 0.38f
    val contentColor = contentColorFor(containerColor)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(contentAlpha)
            .clickable(enabled = enabled) { onCheckedChange?.invoke(!checked) },
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                modifier = Modifier.weight(1f),
            )

            Switch(
                checked = checked,
                onCheckedChange = { it -> if (enabled) onCheckedChange?.invoke(it) },
                enabled = enabled,
            )
        }
    }
}
