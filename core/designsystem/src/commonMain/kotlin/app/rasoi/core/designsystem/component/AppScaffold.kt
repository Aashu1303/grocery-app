package app.rasoi.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.designsystem.theme.FadeThrough

/**
 * App chrome whose bars fade in and out with [barsVisible]. The content always gets the whole area and receives the
 * bar heights as padding, so hiding or showing the bars never resizes it mid-animation; screens that hide the bars
 * ignore the padding and fill the space themselves.
 */
@Composable
fun AppScaffold(
    barsVisible: Boolean,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = { AnimatedVisibility(barsVisible, enter = FadeThrough.enter, exit = FadeThrough.exit) { topBar() } },
        bottomBar = {
            AnimatedVisibility(barsVisible, enter = FadeThrough.enter, exit = FadeThrough.exit) { bottomBar() }
        },
        containerColor = AppTheme.colors.surface,
        contentColor = AppTheme.colors.onSurface,
        // The app applies the system insets once at its root.
        contentWindowInsets = WindowInsets(),
        content = content,
    )
}
