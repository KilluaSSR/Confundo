package killua.dev.confundo.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import killua.dev.confundo.ui.theme.Dimens
import killua.dev.confundo.ui.theme.Spacing

@Composable
fun CardSwitch(
    text: String,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    shape: Shape = MaterialTheme.shapes.large,
    enabled: Boolean = true,
) {
    // 禁用态用 M3 规范的 on-surface 38% 内容色（保留可读性与语义），而非对整卡片做 alpha。
    val baseContentColor = contentColorFor(containerColor)
    val contentColor = if (enabled) baseContentColor else baseContentColor.copy(alpha = 0.38f)

    val toggleModifier = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(toggleModifier)
                .heightIn(min = Dimens.MinTouchTarget)
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
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
                // 点击已由外层 toggleable 统一处理，避免重复语义。
                onCheckedChange = null,
                enabled = enabled,
                thumbContent = {
                    AnimatedContent(
                        targetState = checked,
                        label = "switchThumb",
                    ) { isChecked ->
                        Icon(
                            imageVector = if (isChecked) Icons.Rounded.Check else Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                },
            )
        }
    }
}
