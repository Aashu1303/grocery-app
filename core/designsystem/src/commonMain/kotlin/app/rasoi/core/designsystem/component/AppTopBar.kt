package app.rasoi.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.rasoi.core.designsystem.icon.AppIcons
import app.rasoi.core.designsystem.resources.Res
import app.rasoi.core.designsystem.resources.ds_back
import app.rasoi.core.designsystem.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Screen title bar with an optional back action and trailing [actions]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(text = title, style = AppTheme.type.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = AppIcons.Back, contentDescription = stringResource(Res.string.ds_back))
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppTheme.colors.surface,
            titleContentColor = AppTheme.colors.onSurface,
            navigationIconContentColor = AppTheme.colors.onSurface,
            actionIconContentColor = AppTheme.colors.onSurface,
        ),
    )
}
