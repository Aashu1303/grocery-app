package app.rasoi.core.designsystem.brand

import androidx.compose.runtime.Immutable

/**
 * One frame of the brand intro. Positions are in the 108-unit canvas of [BrandGeometry]; [anchorX], [anchorY] is the
 * canvas point drawn at the centre of the screen and [scale] is dp per canvas unit.
 */
@Immutable
internal data class BrandIntroFrame(
    val scale: Float,
    val anchorX: Float,
    val anchorY: Float,
    val leafAlpha: Float,
    val leafRotation: Float,
    val leafX: Float,
    val leafY: Float,
    val wordReveal: Float,
    val wordAlpha: Float,
    val taglineAlpha: Float,
)
