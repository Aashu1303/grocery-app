package app.rasoi.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import app.rasoi.core.model.Freshness

/** Semantic colours, named by meaning. Every `on*` colour meets WCAG AA against its pair. */
@Immutable
data class AppColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
    val owedToYou: Color,
    val youOwe: Color,
    val isDark: Boolean,
) {
    fun freshness(freshness: Freshness): Color = when (freshness) {
        Freshness.FRESH -> success
        Freshness.USE_SOON -> warning
        Freshness.EXPIRED -> danger
    }

    fun freshnessContainer(freshness: Freshness): Pair<Color, Color> = when (freshness) {
        Freshness.FRESH -> successContainer to onSuccessContainer
        Freshness.USE_SOON -> warningContainer to onWarningContainer
        Freshness.EXPIRED -> dangerContainer to onDangerContainer
    }
}
