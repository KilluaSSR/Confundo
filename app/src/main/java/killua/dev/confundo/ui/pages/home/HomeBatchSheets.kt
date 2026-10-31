package killua.dev.confundo.ui.pages.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import killua.dev.arsenal.material.ArsenalTokens
import killua.dev.arsenal.material.templates.settings.SettingsItem
import killua.dev.arsenal.material.templates.settings.groupedItemShape
import killua.dev.arsenal.material.templates.settings.itemPosition
import killua.dev.confundo.R
import killua.dev.confundo.ui.components.ConfundoSurfaces

@Composable
internal fun BatchActionsSheet(
    selectedCount: Int,
    onJavaOn: () -> Unit,
    onJavaOff: () -> Unit,
    onNativeOn: () -> Unit,
    onNativeOff: () -> Unit,
    onResetOn: () -> Unit,
    onResetOff: () -> Unit,
    onApplyTemplate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ArsenalTokens.spacing.lg)
            .padding(bottom = ArsenalTokens.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(ArsenalTokens.spacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(ArsenalTokens.spacing.xs)) {
            Text(
                text = stringResource(R.string.batch_sheet_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.toolbar_selected_count, selectedCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        BatchToggleRow(
            label = stringResource(R.string.batch_group_java),
            hint = null,
            onOn = onJavaOn,
            onOff = onJavaOff,
        )
        BatchToggleRow(
            label = stringResource(R.string.batch_group_native),
            hint = stringResource(R.string.batch_native_hint),
            onOn = onNativeOn,
            onOff = onNativeOff,
        )
        BatchToggleRow(
            label = stringResource(R.string.batch_group_reset),
            hint = null,
            onOn = onResetOn,
            onOff = onResetOff,
        )

        Column(verticalArrangement = Arrangement.spacedBy(ArsenalTokens.spacing.xs)) {
            Text(
                text = stringResource(R.string.batch_group_template),
                style = MaterialTheme.typography.titleSmall,
            )
            FilledTonalButton(
                onClick = onApplyTemplate,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = contentColorFor(MaterialTheme.colorScheme.primaryContainer),
                ),
            ) {
                Text(stringResource(R.string.toolbar_apply_template))
            }
        }

        Box(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun BatchToggleRow(
    label: String,
    hint: String?,
    onOn: () -> Unit,
    onOff: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ArsenalTokens.spacing.xs)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ArsenalTokens.spacing.sm)) {
            FilledTonalButton(
                onClick = onOn,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = contentColorFor(MaterialTheme.colorScheme.tertiaryContainer),
                ),
            ) {
                Text(stringResource(R.string.batch_on))
            }
            FilledTonalButton(
                onClick = onOff,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = contentColorFor(MaterialTheme.colorScheme.secondaryContainer),
                ),
            ) {
                Text(stringResource(R.string.batch_off))
            }
        }
    }
}

@Composable
internal fun TemplatePickerSheet(
    templates: List<TemplateItem>,
    onPick: (String) -> Unit,
) {
    Column(
        modifier = Modifier.padding(bottom = ArsenalTokens.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(ArsenalTokens.spacing.xs),
    ) {
        Text(
            text = stringResource(R.string.template_apply_dialog_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = ArsenalTokens.spacing.lg, vertical = ArsenalTokens.spacing.sm),
        )
        Column(
            modifier = Modifier.padding(horizontal = ArsenalTokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ArsenalTokens.spacing.xs),
        ) {
            templates.forEachIndexed { index, template ->
                SettingsItem(
                    title = template.name,
                    shape = groupedItemShape(itemPosition(index, templates.size)),
                    containerColor = ConfundoSurfaces.row,
                    onClick = { onPick(template.id) },
                )
            }
        }
        Box(modifier = Modifier.navigationBarsPadding())
    }
}
