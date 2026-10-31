package killua.dev.confundo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import killua.dev.arsenal.material.ArsenalTokens
import killua.dev.arsenal.material.templates.settings.SettingsGroup
import killua.dev.arsenal.material.templates.settings.groupedItemShape
import killua.dev.arsenal.material.templates.settings.itemPosition

object ConfundoSurfaces {
    val row: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceBright
}

@Immutable
data class RowSlot(val shape: Shape, val color: Color)

@Composable
fun RowGroup(
    rows: List<@Composable (RowSlot) -> Unit>,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    if (rows.isEmpty()) return
    val color = ConfundoSurfaces.row
    SettingsGroup(title = title, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(ArsenalTokens.spacing.xs)) {
            rows.forEachIndexed { index, row ->
                row(RowSlot(groupedItemShape(itemPosition(index, rows.size)), color))
            }
        }
    }
}
