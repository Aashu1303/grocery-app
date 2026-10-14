package app.rasoi.core.designsystem.brand

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.ui.util.lerp
import app.rasoi.core.designsystem.brand.BrandGeometry as G

/**
 * The cold-start brand intro as a pure function of elapsed time (ADR 0027): the र zooms in at the centre, the tadka
 * leaf lands on it, then the र slides left to become the R of "Rasoi" while "asoı" wipes in and the leaf moves over
 * to dot the ı. Frame 0 matches the static platform launch screen exactly.
 */
internal object BrandIntroTimeline {
    const val DURATION_MILLIS = 1200

    /** dp per canvas unit. 240 / 108 is the Android 12 splash icon size and the iOS launch image size. */
    private const val SPLASH_SCALE = 240f / 108f
    private const val ZOOM_SCALE = 340f / 108f
    private const val SETTLE_SCALE = 300f / 108f
    private const val LOCKUP_SCALE = 1.6f

    /** End of the zoom-in, at its peak size. */
    const val ZOOM_END = 280

    /** The leaf has landed and the र has settled. */
    const val LAND_END = 480

    private const val WORD_FADE_END = 650
    private const val TAGLINE_START = 630
    private const val LOCKUP_END = 950

    private const val LEAF_DROP_X = 6f
    private const val LEAF_DROP_Y = -26f
    private const val LEAF_DROP_DEGREES = -50f

    private val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    fun frameAt(elapsedMillis: Int): BrandIntroFrame {
        val t = elapsedMillis.coerceIn(0, DURATION_MILLIS)
        val zoom = progress(t, 0, ZOOM_END, EaseOutBack)
        val land = progress(t, ZOOM_END, LAND_END, LinearOutSlowInEasing)
        val settle = progress(t, ZOOM_END, LAND_END, FastOutSlowInEasing)
        val lockup = progress(t, LAND_END, LOCKUP_END, standard)
        val scale = when {
            t <= ZOOM_END -> lerp(SPLASH_SCALE, ZOOM_SCALE, zoom)
            t <= LAND_END -> lerp(ZOOM_SCALE, SETTLE_SCALE, settle)
            else -> lerp(SETTLE_SCALE, LOCKUP_SCALE, lockup)
        }
        return BrandIntroFrame(
            scale = scale,
            anchorX = lerp(G.CANVAS_CENTRE, G.LOCKUP_CENTRE_X, lockup),
            anchorY = lerp(G.CANVAS_CENTRE, G.LOCKUP_CENTRE_Y, lockup),
            leafAlpha = land,
            leafRotation = lerp(LEAF_DROP_DEGREES, 0f, land),
            leafX = lerp(G.LEAF_CENTRE_X + LEAF_DROP_X * (1 - land), G.LEAF_DOT_X, lockup),
            leafY = lerp(G.LEAF_CENTRE_Y + LEAF_DROP_Y * (1 - land), G.LEAF_DOT_Y, lockup),
            wordReveal = lockup,
            wordAlpha = progress(t, LAND_END, WORD_FADE_END, standard),
            taglineAlpha = progress(t, TAGLINE_START, LOCKUP_END, standard),
        )
    }

    private fun progress(t: Int, start: Int, end: Int, easing: Easing): Float =
        easing.transform(((t - start).toFloat() / (end - start)).coerceIn(0f, 1f))
}
