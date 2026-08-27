package killua.dev.confundo.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import killua.dev.confundo.ui.theme.Spacing

/** 详情页分区标题。 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    startPadding: Dp = Spacing.xl,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(
            start = startPadding,
            end = Spacing.xl,
            top = Spacing.lgIncreased,
            bottom = Spacing.xs,
        ),
    )
}
