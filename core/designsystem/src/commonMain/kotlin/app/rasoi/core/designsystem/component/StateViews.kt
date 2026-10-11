package app.rasoi.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import app.rasoi.core.designsystem.icon.AppIcons
import app.rasoi.core.designsystem.resources.Res
import app.rasoi.core.designsystem.resources.ds_loading
import app.rasoi.core.designsystem.resources.ds_try_again
import app.rasoi.core.designsystem.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

private const val SKELETON_ROWS = 4
private const val SKELETON_TITLE_WIDTH = 0.55f
private const val SKELETON_DETAIL_WIDTH = 0.35f
private const val SKELETON_BAR_ALPHA = 0.5f

/**
 * Skeleton placeholder shown while a screen loads: list-card shapes inset like the real rows. It waits one short
 * duration before fading in, so a load that finishes quickly never flashes a placeholder during navigation.
 */
@Composable
fun LoadingView(modifier: Modifier = Modifier, rows: Int = SKELETON_ROWS) {
    val description = stringResource(Res.string.ds_loading)
    val motion = AppTheme.motion
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, tween(durationMillis = motion.shortMillis, delayMillis = motion.shortMillis))
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = reveal.value }
            .padding(horizontal = AppTheme.spacing.screenPadding, vertical = AppTheme.spacing.sm)
            .clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        repeat(rows) { SkeletonCard() }
    }
}

@Composable
private fun SkeletonCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppTheme.spacing.xxl + AppTheme.spacing.md)
            .background(AppTheme.colors.surfaceVariant.copy(alpha = SKELETON_BAR_ALPHA), AppTheme.shapes.card)
            .padding(AppTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs, Alignment.CenterVertically),
    ) {
        SkeletonBar(Modifier.fillMaxWidth(SKELETON_TITLE_WIDTH))
        SkeletonBar(Modifier.fillMaxWidth(SKELETON_DETAIL_WIDTH))
    }
}

@Composable
private fun SkeletonBar(modifier: Modifier) {
    Box(modifier.height(AppTheme.spacing.sm).background(AppTheme.colors.surfaceVariant, AppTheme.shapes.small))
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
            .padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}
