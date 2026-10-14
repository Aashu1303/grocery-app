package app.rasoi.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import app.rasoi.core.designsystem.brand.BrandIntroTimeline
import kotlinx.coroutines.delay

/**
 * Plays [BrandIntro] once, then calls [onFinished]. It never waits for anything else, so the app keeps loading
 * underneath. With animations turned off in system settings the animation ends at once. [startDelayMillis] holds the
 * first frame while a platform launch screen is still fading out over it.
 */
@Composable
fun AnimatedBrandIntro(onFinished: () -> Unit, modifier: Modifier = Modifier, startDelayMillis: Long = 0) {
    val elapsed = remember { Animatable(0f) }
    val finish by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        // Start the clock after the first frame: the app's start-up work lands on that frame, and the zoom should not
        // lose its opening to it. Until then the intro shows frame 0, which matches the launch screen.
        withFrameNanos { }
        delay(startDelayMillis)
        elapsed.animateTo(
            targetValue = BrandIntroTimeline.DURATION_MILLIS.toFloat(),
            animationSpec = tween(durationMillis = BrandIntroTimeline.DURATION_MILLIS, easing = LinearEasing),
        )
        finish()
    }
    BrandIntroContent(elapsedMillis = { elapsed.value.toInt() }, modifier = modifier)
}
