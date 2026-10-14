package app.rasoi.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import app.rasoi.core.designsystem.icon.AppIcons
import app.rasoi.core.designsystem.resources.Res
import app.rasoi.core.designsystem.resources.ds_back
import app.rasoi.core.designsystem.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Screen title bar with an optional back action and trailing [actions]. The title keeps the screen padding (16 dp)
 * above, below and before it, and the bar grows for a two-line title instead of crowding it against the status bar.
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val spacing = AppTheme.spacing
    // A 48 dp icon button draws its 24 dp icon 12 dp in, so a 4 dp inset puts the icon on the 16 dp screen edge.
    val iconInset = spacing.xxs
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surface)
            .heightIn(min = spacing.minTouchTarget + spacing.md)
            .padding(start = if (onBack == null) spacing.screenPadding else iconInset, end = iconInset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = AppIcons.Back,
                    contentDescription = stringResource(Res.string.ds_back),
                    tint = AppTheme.colors.onSurface,
                )
            }
        }
        Text(
            text = title,
            style = AppTheme.type.title,
            color = AppTheme.colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .then(if (onBack == null) Modifier else Modifier.padding(start = iconInset))
                .padding(end = spacing.xs)
                .padding(vertical = spacing.screenPadding)
                .semantics { heading() },
        )
        actions()
    }
}
