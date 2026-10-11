package app.rasoi.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.model.Freshness

/** Pantry list entry: name, quantity and location, with an expiry chip. Texts arrive already localised. */
@Composable
fun PantryItemRow(
    name: String,
    quantityText: String,
    locationText: String,
    freshness: Freshness,
    expiryText: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    ListRow(
        modifier = modifier,
        onClick = onClick,
        leading = {
            Text(text = name, style = AppTheme.type.body, color = AppTheme.colors.onSurface)
            Text(
                text = "$quantityText · $locationText",
                style = AppTheme.type.caption,
                color = AppTheme.colors.onSurfaceVariant,
            )
        },
        trailing = { ExpiryChip(freshness = freshness, label = expiryText) },
    )
}
