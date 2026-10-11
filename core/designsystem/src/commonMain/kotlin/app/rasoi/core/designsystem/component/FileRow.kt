package app.rasoi.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.rasoi.core.designsystem.icon.AppIcons
import app.rasoi.core.designsystem.theme.AppTheme

/** A picked file with a detail line and an optional remove button labelled [removeLabel] for screen readers. */
@Composable
fun FileRow(
    name: String,
    detail: String,
    removeLabel: String,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null,
) {
    ListRow(
        modifier = modifier,
        onClick = null,
        leading = {
            Text(text = name, style = AppTheme.type.body, color = AppTheme.colors.onSurface)
            Text(text = detail, style = AppTheme.type.caption, color = AppTheme.colors.onSurfaceVariant)
        },
        trailing = {
            if (onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = removeLabel,
                        tint = AppTheme.colors.onSurface,
                    )
                }
            }
        },
    )
}
