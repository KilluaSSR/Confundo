package killua.dev.confundo.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import killua.dev.confundo.R
import killua.dev.confundo.ui.theme.Dimens
import killua.dev.confundo.ui.theme.ConfundoTheme
import killua.dev.confundo.ui.theme.ShapeRadius
import killua.dev.confundo.ui.theme.Spacing

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
internal fun animatedGroupedShape(
    position: AppPosition,
    selected: Boolean,
    cornerRadius: Dp,
): RoundedCornerShape {
    val inner = ShapeRadius.ExtraSmall
    val targetTop = if (selected || position == AppPosition.Single || position == AppPosition.Top) {
        cornerRadius
    } else {
        inner
    }
    val targetBottom =
        if (selected || position == AppPosition.Single || position == AppPosition.Bottom) {
            cornerRadius
        } else {
            inner
        }
    val shapeSpec = spring<Dp>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
    )
    val topRadius by animateDpAsState(targetTop, shapeSpec, label = "groupTopRadius")
    val bottomRadius by animateDpAsState(targetBottom, shapeSpec, label = "groupBottomRadius")
    return RoundedCornerShape(
        topStart = topRadius,
        topEnd = topRadius,
        bottomStart = bottomRadius,
        bottomEnd = bottomRadius,
    )
}

@Composable
internal fun animatedGroupedColor(selected: Boolean): Color {
    val target = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceBright
    }
    return animateColorAsState(
        targetValue = target,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "groupContainerColor",
    ).value
}

@Composable
fun AppList(
    modifier: Modifier = Modifier,
    apps: List<AppItemData>,
    cornerRadius: Dp = Dimens.ListCorner,
    selectedPkgs: Set<String> = emptySet(),
    onClick: (String) -> Unit = {},
    onLongClick: (String) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.xxs),
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
        }
    }
}

@Composable
fun AppListRow(
    appData: AppItemData,
    position: AppPosition,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Dimens.ListCorner,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val containerColor = animatedGroupedColor(selected)
    val shape = animatedGroupedShape(position, selected, cornerRadius)

    val stateText = stringResource(
        if (appData.isSpoofingEnabled) R.string.cd_app_enabled else R.string.cd_app_disabled
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = containerColor,
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.MinTouchTarget)
                .clip(shape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = "${appData.appName}, ${appData.packageName}"
                    stateDescription = stateText
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (appData.iconBitmap != null) {
                    Image(
                        bitmap = appData.iconBitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = appData.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.lg))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appData.appName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(Spacing.xxs))
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
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
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
    ConfundoTheme(dynamicColor = false) {
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
            modifier = Modifier.padding(vertical = Spacing.md),
            color = MaterialTheme.colorScheme.background
        ) {
            AppList(apps = fakeApps)
        }
    }
}
