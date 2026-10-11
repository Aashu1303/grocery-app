package app.rasoi.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Corner radii and elevations (design-plan 7.1): cards 12, sheets 16; mostly flat. */
@Immutable
data class AppShapes(
    val radiusSm: Dp = 8.dp,
    val radiusMd: Dp = 12.dp,
    val radiusLg: Dp = 16.dp,
    val elevationNone: Dp = 0.dp,
    val elevationLow: Dp = 1.dp,
    val elevationHigh: Dp = 3.dp,
) {
    val small: RoundedCornerShape get() = RoundedCornerShape(radiusSm)
    val card: RoundedCornerShape get() = RoundedCornerShape(radiusMd)
    val sheet: RoundedCornerShape get() = RoundedCornerShape(topStart = radiusLg, topEnd = radiusLg)
    val pill: RoundedCornerShape get() = RoundedCornerShape(percent = 50)
}
