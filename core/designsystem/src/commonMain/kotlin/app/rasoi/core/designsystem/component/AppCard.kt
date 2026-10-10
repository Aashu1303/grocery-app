package app.rasoi.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.rasoi.core.designsystem.theme.AppTheme

/** Flat container for grouped content; clickable when [onClick] is set. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(
        containerColor = AppTheme.colors.surface,
        contentColor = AppTheme.colors.onSurface,
    )
    val border = BorderStroke(AppTheme.shapes.elevationLow, AppTheme.colors.surfaceVariant)
    val body: @Composable ColumnScope.() -> Unit = {
        Column(modifier = Modifier.padding(AppTheme.spacing.md), content = content)
    }
    if (onClick == null) {
        Card(modifier = modifier, shape = AppTheme.shapes.card, colors = colors, border = border, content = body)
    } else {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = AppTheme.shapes.card,
            colors = colors,
            border = border,
            content = body,
        )
    }
}
