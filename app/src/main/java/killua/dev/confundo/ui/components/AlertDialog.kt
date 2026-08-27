package killua.dev.confundo.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import killua.dev.confundo.ui.theme.ConfundoTheme
import killua.dev.confundo.ui.theme.ShapeRadius

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConfirmationDialog(
    show: Boolean,
    title: String,
    body: String,
    confirmText: String,
    dismissText: String = "",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(confirmText)
            }
        },
        dismissButton = if (dismissText.isNotEmpty()) {
            {
                TextButton(
                    onClick = onDismiss,
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(dismissText)
                }
            }
        } else null,
        shape = RoundedCornerShape(ShapeRadius.ExtraLarge),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(showBackground = true)
@Composable
private fun ConfirmationDialogPreview() {
    ConfundoTheme(dynamicColor = false) {
        ConfirmationDialog(
            show = true,
            title = "Permission Required",
            body = "The Camera permission has been denied. Please grant it in system settings.",
            confirmText = "Open Settings",
            dismissText = "Cancel",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
