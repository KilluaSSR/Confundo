package killua.dev.confundo.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AppPosition { Single, Top, Middle, Bottom }

@Immutable
data class AppItemData(
    val id: String,
    val icon: ImageVector,
    val iconBitmap: ImageBitmap? = null,
    val appName: String,
    val packageName: String,
    val isSpoofingEnabled: Boolean = false,
)

@Composable
fun AppList(
    modifier: Modifier = Modifier,
    apps: List<AppItemData>,
    cornerRadius: Dp = 20.dp,
    selectedPkgs: Set<String> = emptySet(),
    onClick: (String) -> Unit = {},
    onLongClick: (String) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        apps.forEachIndexed { index, appData ->
            val position = when {
                apps.size <= 1 -> AppPosition.Single
                index == 0 -> AppPosition.Top
                index == apps.lastIndex -> AppPosition.Bottom
                else -> AppPosition.Middle
            }
            AppListRow(
                appData = appData,
                position = position,
                cornerRadius = cornerRadius,
                selected = appData.packageName in selectedPkgs,
                onClick = { onClick(appData.packageName) },
                onLongClick = { onLongClick(appData.packageName) },
            )
            if (index < apps.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun AppListRow(
    appData: AppItemData,
    position: AppPosition,
    cornerRadius: Dp = 20.dp,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
    }

    val shape = when (position) {
        AppPosition.Single -> RoundedCornerShape(cornerRadius)
        AppPosition.Top -> RoundedCornerShape(
            topStart = cornerRadius,
            topEnd = cornerRadius,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        )
        AppPosition.Middle -> RoundedCornerShape(0.dp)
        AppPosition.Bottom -> RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = cornerRadius,
            bottomEnd = cornerRadius
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (appData.iconBitmap != null) {
                    Image(
                        bitmap = appData.iconBitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = appData.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appData.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = appData.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (appData.isSpoofingEnabled) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun AppListPreview() {
    MaterialTheme {
        val fakeApps = listOf(
            AppItemData(
                "1",
                Icons.Rounded.Android,
                null,
                "PrivacyLens",
                "killua.dev.privacylens",
                isSpoofingEnabled = true,
            ),
            AppItemData(
                "2", Icons.Rounded.Android, null, "Signal", "org.thoughtcrime.securesms",
            ),
        )
        Surface(
            modifier = Modifier.padding(vertical = 12.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            AppList(apps = fakeApps)
        }
    }
}
