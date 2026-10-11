package app.rasoi.core.designsystem.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

private const val FADE_OUT_MILLIS = 90
private const val FADE_IN_MILLIS = 220

/**
 * Material fade-through: the outgoing content is fully gone before the incoming one starts to appear, so text from
 * two screens never shows at once. Used for screen changes and for the app bars that come and go with them.
 */
object FadeThrough {
    val enter: EnterTransition = fadeIn(tween(FADE_IN_MILLIS, delayMillis = FADE_OUT_MILLIS))
    val exit: ExitTransition = fadeOut(tween(FADE_OUT_MILLIS))
}
