package killua.dev.confundo.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

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
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = if (dismissText.isNotEmpty()) {
            {
                TextButton(onClick = onDismiss) {
                    Text(dismissText)
                }
            }
        } else null,
    )
}

@Preview(showBackground = true)
@Composable
private fun ConfirmationDialogPreview() {
    MaterialTheme {
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
