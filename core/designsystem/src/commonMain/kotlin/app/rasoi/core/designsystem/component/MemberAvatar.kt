package app.rasoi.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import app.rasoi.core.designsystem.theme.AppTheme

/** Initials in a circle; announced as the member's name. */
@Composable
fun MemberAvatar(name: String, modifier: Modifier = Modifier, size: AvatarSize = AvatarSize.MEDIUM) {
    val diameter = when (size) {
        AvatarSize.SMALL -> AppTheme.spacing.lg
        AvatarSize.MEDIUM -> AppTheme.spacing.xl + AppTheme.spacing.xs
    }
    Box(
        modifier = modifier
            .size(diameter)
            .background(AppTheme.colors.primaryContainer, CircleShape)
            .clearAndSetSemantics { contentDescription = name },
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
