package app.rasoi.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable

/** Two durations and one easing; no decorative animation (design-plan 7.1). */
@Immutable
data class AppMotion(
    val shortMillis: Int = 150,
    val mediumMillis: Int = 250,
    val easing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
)
