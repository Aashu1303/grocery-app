package app.rasoi.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import app.rasoi.core.designsystem.theme.AppColors
import app.rasoi.core.designsystem.tokens.DarkColors
import app.rasoi.core.designsystem.tokens.LightColors
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

class ContrastTest {
    private fun ratio(a: Color, b: Color): Float {
        val (la, lb) = a.luminance() to b.luminance()
        return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
    }

    private fun textPairs(c: AppColors) = mapOf(
        "onPrimary/primary" to (c.onPrimary to c.primary),
        "onPrimaryContainer/primaryContainer" to (c.onPrimaryContainer to c.primaryContainer),
        "onSurface/surface" to (c.onSurface to c.surface),
        "onSurfaceVariant/surface" to (c.onSurfaceVariant to c.surface),
        "onSurfaceVariant/surfaceVariant" to (c.onSurfaceVariant to c.surfaceVariant),
        "primary/surface" to (c.primary to c.surface),
        "success/surface" to (c.success to c.surface),
        "warning/surface" to (c.warning to c.surface),
        "danger/surface" to (c.danger to c.surface),
        "onDanger/danger" to (c.onDanger to c.danger),
        "owedToYou/surface" to (c.owedToYou to c.surface),
        "youOwe/surface" to (c.youOwe to c.surface),
        "onSuccessContainer/successContainer" to (c.onSuccessContainer to c.successContainer),
        "onWarningContainer/warningContainer" to (c.onWarningContainer to c.warningContainer),
        "onDangerContainer/dangerContainer" to (c.onDangerContainer to c.dangerContainer),
    )

    @Test
    fun textMeetsWcagAaInBothThemes() {
        listOf("light" to LightColors, "dark" to DarkColors).forEach { (theme, colors) ->
            textPairs(colors).forEach { (name, pair) ->
                val value = ratio(pair.first, pair.second)
                assertTrue(value >= TEXT_MINIMUM, "$theme $name contrast $value < $TEXT_MINIMUM")
            }
            val outline = ratio(colors.outline, colors.surface)
            assertTrue(outline >= NON_TEXT_MINIMUM, "$theme outline contrast $outline < $NON_TEXT_MINIMUM")
        }
    }

    private companion object {
        const val TEXT_MINIMUM = 4.5f
        const val NON_TEXT_MINIMUM = 3f
    }
}
