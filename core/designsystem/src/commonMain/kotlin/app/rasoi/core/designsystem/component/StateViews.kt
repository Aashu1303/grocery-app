package app.rasoi.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import app.rasoi.core.designsystem.icon.AppIcons
import app.rasoi.core.designsystem.resources.Res
import app.rasoi.core.designsystem.resources.ds_loading
import app.rasoi.core.designsystem.resources.ds_try_again
import app.rasoi.core.designsystem.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

private const val SKELETON_SECONDARY_WIDTH = 0.6f

/** Skeleton placeholder shown while a screen loads. */
@Composable
fun LoadingView(modifier: Modifier = Modifier, rows: Int = 3) {
    val description = stringResource(Res.string.ds_loading)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppTheme.spacing.screenPadding)
            .clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        repeat(rows) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                SkeletonBar(Modifier.fillMaxWidth())
                SkeletonBar(Modifier.fillMaxWidth(SKELETON_SECONDARY_WIDTH))
            }
        }
    }
}

@Composable
private fun SkeletonBar(modifier: Modifier) {
    Box(modifier.height(AppTheme.spacing.md).background(AppTheme.colors.surfaceVariant, AppTheme.shapes.small))
}

/** Nothing to show yet; optionally offers the screen's primary action. */
@Composable
fun EmptyView(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    CenteredMessage(modifier = modifier) {
        Text(
            text = title,
            style = AppTheme.type.title,
            color = AppTheme.colors.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = AppTheme.type.body,
            color = AppTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) AppButton(text = actionLabel, onClick = onAction)
    }
}

/** Something failed; always offers a retry. */
@Composable
fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    CenteredMessage(modifier = modifier) {
        Icon(imageVector = AppIcons.Warning, contentDescription = null, tint = AppTheme.colors.danger)
        Text(
            text = message,
            style = AppTheme.type.body,
            color = AppTheme.colors.onSurface,
            textAlign = TextAlign.Center,
        )
        AppButton(
            text = stringResource(Res.string.ds_try_again),
            onClick = onRetry,
            variant = AppButtonVariant.SECONDARY,
        )
    }
}

@Composable
private fun CenteredMessage(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}
