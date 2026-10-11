package app.rasoi.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.Dp
import app.rasoi.core.designsystem.theme.AppTheme

/** Initials in a circle; announced as the member's name. With [onClick] it becomes a 48 dp button. */
@Composable
fun MemberAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: AvatarSize = AvatarSize.MEDIUM,
    onClick: (() -> Unit)? = null,
) {
    val diameter = when (size) {
        AvatarSize.SMALL -> AppTheme.spacing.lg
        AvatarSize.MEDIUM -> AppTheme.spacing.xl + AppTheme.spacing.xs
    }
    val clickable = if (onClick != null) {
        Modifier
            .sizeIn(minWidth = AppTheme.spacing.minTouchTarget, minHeight = AppTheme.spacing.minTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }
    Box(
        modifier = modifier.then(clickable).clearAndSetSemantics {
            contentDescription = name
            if (onClick != null) {
                role = Role.Button
                onClick(label = null) {
                    onClick()
                    true
                }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        AvatarCircle(name, diameter, size)
    }
}

@Composable
private fun AvatarCircle(name: String, diameter: Dp, size: AvatarSize) {
    Box(
        modifier = Modifier.size(diameter).background(AppTheme.colors.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsOf(name),
            style = if (size == AvatarSize.SMALL) AppTheme.type.caption else AppTheme.type.label,
            color = AppTheme.colors.onPrimaryContainer,
        )
    }
}

internal fun initialsOf(name: String): String = name.trim()
    .split(Regex("\\s+"))
    .filter { it.isNotEmpty() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
