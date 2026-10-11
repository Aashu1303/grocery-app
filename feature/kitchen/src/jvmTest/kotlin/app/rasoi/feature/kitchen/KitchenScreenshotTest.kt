package app.rasoi.feature.kitchen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.DpSize
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.domain.kitchen.DishSuggestion
import app.rasoi.core.domain.kitchen.UseFirst
import app.rasoi.core.model.Category
import app.rasoi.core.model.Quantity
import app.rasoi.core.testing.Builders
import io.github.takahirom.roborazzi.captureRoboImage
import kotlinx.datetime.LocalDate
import kotlin.test.Test

/** Every kitchen state, light and dark (design-plan 7.3: loading, empty, error, content). */
@OptIn(ExperimentalTestApi::class)
class KitchenScreenshotTest {
    private val today = LocalDate(2026, 10, 11)
    private val entries = UseFirst.rank(
        listOf(
            Builders.pantryItem(
                "p1",
                "Amul Taaza Toned Milk",
                Category.DAIRY,
                Quantity.millilitres(1_000),
                "l1",
                LocalDate(2026, 10, 9),
            ),
            Builders.pantryItem("p2", "Paneer", Category.DAIRY, Quantity.grams(200), "l1", LocalDate(2026, 10, 12)),
            Builders.pantryItem(
                "p3",
                "Beans Lobia",
                Category.PRODUCE,
                Quantity.grams(360),
                "l1",
                LocalDate(2026, 10, 15),
            ),
            Builders.pantryItem("p4", "Basmati Rice", Category.STAPLES, Quantity.kilograms(5), "l2", null),
        ),
        today,
    )
    private val content = KitchenUiState.Content(entries)
    private val dishes = listOf(
        DishSuggestion(
            "Paneer bhurji with rice",
            listOf("paneer", "basmati rice"),
            "Crumble and fry the paneer with spices, serve over rice.",
        ),
        DishSuggestion("Lobia curry", listOf("beans lobia"), "Simmer the beans with onion and tomato until soft."),
    )

    private val screens: List<Pair<String, @Composable () -> Unit>> = listOf(
        "kitchen_loading" to { KitchenScreen(KitchenUiState.Loading, {}) },
        "kitchen_error" to { KitchenScreen(KitchenUiState.Error(KitchenError.OFFLINE), {}) },
        "kitchen_empty" to { KitchenScreen(KitchenUiState.Content(emptyList()), {}) },
        "kitchen_list" to { KitchenScreen(content, {}) },
        "kitchen_ideas_loading" to { KitchenScreen(content.copy(ideas = DishIdeas.Loading), {}) },
        "kitchen_ideas_ready" to { KitchenScreen(content.copy(ideas = DishIdeas.Ready(dishes)), {}) },
        "kitchen_ideas_none" to { KitchenScreen(content.copy(ideas = DishIdeas.Ready(emptyList())), {}) },
        "kitchen_ideas_unavailable" to { KitchenScreen(content.copy(ideas = DishIdeas.Unavailable), {}) },
        "kitchen_ideas_failed" to
            { KitchenScreen(content.copy(ideas = DishIdeas.Failed(KitchenError.IDEAS_FAILED)), {}) },
    )

    @Test
    fun everyScreenStateInLightAndDark() {
        screens.forEach { (name, screen) ->
            listOf(false, true).forEach { dark ->
                runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
                    setContent {
                        AppTheme(darkTheme = dark) {
                            Box(
                                Modifier.testTag(TAG)
                                    .size(DpSize(AppTheme.spacing.xxl * 8, AppTheme.spacing.xxl * 14))
                                    .background(AppTheme.colors.surface),
                            ) {
                                screen()
                            }
                        }
                    }
                    onNodeWithTag(TAG).captureRoboImage("screenshots/${name}_${if (dark) "dark" else "light"}.png")
                }
            }
        }
    }

    private companion object {
        const val TAG = "screen"
        const val WIDTH = 420
        const val HEIGHT = 720
    }
}
