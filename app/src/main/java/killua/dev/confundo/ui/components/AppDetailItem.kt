package killua.dev.confundo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import killua.dev.confundo.ui.theme.Dimens
import killua.dev.confundo.ui.theme.Spacing

@Composable
fun AppDetailItem(
    title: String,
    content: String,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    // 禁用态：用 M3 规范内容色 + disabled 语义，而非整体 alpha（后者不向无障碍声明状态、且降低对比度）。
    val disabledAlpha = 0.38f
    val titleColor = if (enabled) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurface.copy(alpha = disabledAlpha)
    val contentColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = disabledAlpha)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget)
            .then(
                if (enabled) Modifier
                    .clickable(onClick = onClick)
                    .semantics { role = Role.Button }
                else Modifier.semantics { disabled() }
            )
            .padding(horizontal = Spacing.xl, vertical = Spacing.md),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = titleColor,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = content.ifEmpty { "—" },
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor
        )
    }
}
