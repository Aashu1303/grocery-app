package app.rasoi.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.rasoi.core.designsystem.brand.BrandGeometry
import app.rasoi.core.designsystem.brand.BrandIntroFrame
import app.rasoi.core.designsystem.brand.BrandIntroTimeline
import app.rasoi.core.designsystem.resources.Res
import app.rasoi.core.designsystem.resources.ds_brand_name
import app.rasoi.core.designsystem.resources.ds_brand_tagline
import app.rasoi.core.designsystem.theme.AppColors
import app.rasoi.core.designsystem.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/**
 * One frame of the Rasoi brand intro, [elapsedMillis] after launch (ADR 0027). Stateless so every frame can be
 * previewed and screenshot-tested; [AnimatedBrandIntro] plays it. Fill the whole window: the mark is centred on the
 * window, like the platform launch screen it continues from.
 */
@Composable
fun BrandIntro(elapsedMillis: Int, modifier: Modifier = Modifier) {
    BrandIntroContent(elapsedMillis = { elapsedMillis }, modifier = modifier)
}

/**
 * Reads [elapsedMillis] only in the draw and layer phases, so playing the intro redraws each frame without
 * recomposing or re-laying out anything.
 */
@Composable
internal fun BrandIntroContent(elapsedMillis: () -> Int, modifier: Modifier = Modifier) {
    val paths = remember { BrandPaths() }
    val colors = AppTheme.colors
    val name = stringResource(Res.string.ds_brand_name)
    Box(modifier.background(colors.surface)) {
        Canvas(Modifier.fillMaxSize().semantics { contentDescription = name }) {
            drawBrand(BrandIntroTimeline.frameAt(elapsedMillis()), paths, colors)
        }
        AppText(
            text = stringResource(Res.string.ds_brand_tagline),
            color = colors.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = AppTheme.spacing.xxl)
                .graphicsLayer {
                    val shown = BrandIntroTimeline.frameAt(elapsedMillis()).taglineAlpha
                    alpha = shown
                    translationY = (1 - shown) * TAGLINE_RISE_DP * density
                },
        )
    }
}

private const val TAGLINE_RISE_DP = 16

@Immutable
private class BrandPaths {
    val glyph: Path = parse(BrandGeometry.GLYPH)
    val word: Path = parse(BrandGeometry.WORD)
    val leaf: Path = parse(BrandGeometry.LEAF)
    val vein: Path = parse(BrandGeometry.LEAF_VEIN)

    private fun parse(data: String): Path = PathParser().parsePathString(data).toPath()
}

private fun DrawScope.drawBrand(frame: BrandIntroFrame, paths: BrandPaths, colors: AppColors) {
    val unit = frame.scale * density
    withTransform({
        translate(center.x, center.y)
        scale(unit, unit, Offset.Zero)
        translate(-frame.anchorX, -frame.anchorY)
    }) {
        drawPath(paths.glyph, colors.primary)
        if (frame.wordReveal > 0f) {
            val right = BrandGeometry.WORD_X + BrandGeometry.WORD_WIDTH * BrandGeometry.WORD_SCALE * frame.wordReveal
            clipRect(left = BrandGeometry.WORD_X, top = 0f, right = right, bottom = size.height) {
                withTransform({
                    translate(BrandGeometry.WORD_X, BrandGeometry.WORD_Y)
                    scale(BrandGeometry.WORD_SCALE, BrandGeometry.WORD_SCALE, Offset.Zero)
                }) { drawPath(paths.word, colors.primary, alpha = frame.wordAlpha) }
            }
        }
        if (frame.leafAlpha > 0f) {
            withTransform({
                translate(frame.leafX, frame.leafY)
                rotate(frame.leafRotation, Offset.Zero)
                translate(-BrandGeometry.LEAF_CENTRE_X, -BrandGeometry.LEAF_CENTRE_Y)
            }) {
                drawPath(paths.leaf, colors.brandAccent, alpha = frame.leafAlpha)
                drawPath(
                    paths.vein,
                    colors.onBrandAccent,
                    alpha = frame.leafAlpha,
                    style = Stroke(width = BrandGeometry.LEAF_VEIN_WIDTH, cap = StrokeCap.Round),
                )
            }
        }
    }
}
