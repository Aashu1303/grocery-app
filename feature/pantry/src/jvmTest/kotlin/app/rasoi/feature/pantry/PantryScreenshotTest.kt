package app.rasoi.feature.pantry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import app.rasoi.core.designsystem.component.BottomSheetSurface
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.model.Category
import app.rasoi.core.model.LocationId
import app.rasoi.core.model.LocationKind
import app.rasoi.core.model.Quantity
import app.rasoi.core.testing.Builders
import io.github.takahirom.roborazzi.captureRoboImage
import kotlinx.datetime.LocalDate
import kotlin.test.Test

/** Every pantry state, light and dark (design-plan 7.3: loading, empty, error, content). */
@OptIn(ExperimentalTestApi::class)
class PantryScreenshotTest {
    private val today = LocalDate(2026, 10, 11)
    private val fridge = Builders.location("l1", "Fridge", LocationKind.FRIDGE)
    private val shelf = Builders.location("l2", "Shelf", LocationKind.SHELF)
    private val items = listOf(
        Builders.pantryItem(
            "p1",
            "Amul Taaza Toned Milk",
            Category.DAIRY,
            Quantity.millilitres(1_000),
            "l1",
            LocalDate(2026, 10, 9),
        ),
        Builders.pantryItem("p2", "Beans Lobia", Category.PRODUCE, Quantity.grams(360), "l1", LocalDate(2026, 10, 13)),
        Builders.pantryItem("p3", "Paneer", Category.DAIRY, Quantity.grams(200), "l1", LocalDate(2026, 10, 16)),
        Builders.pantryItem("p4", "Toor Dal", Category.STAPLES, Quantity.kilograms(1), "l2", null),
    )
    private val content = PantryUiState.Content(items, listOf(fridge, shelf), today, canEdit = true)

    private fun sheetDetails(error: PantryError? = null) = PantrySheet.Details(items[1].id, error = error)

    private val largeTextScreens = setOf("pantry_list", "pantry_add_form", "pantry_item_details")

    private val screens: List<Pair<String, @Composable () -> Unit>> = listOf(
        "pantry_loading" to { PantryScreen(PantryUiState.Loading, {}) },
        "pantry_error" to { PantryScreen(PantryUiState.Error(PantryError.OFFLINE), {}) },
        "pantry_empty" to { PantryScreen(content.copy(items = emptyList()), {}) },
        "pantry_list" to { PantryScreen(content, {}) },
        "pantry_filtered" to { PantryScreen(content.copy(filter = LocationId("l2")), {}) },
        "pantry_filtered_empty" to {
            PantryScreen(content.copy(items = items.take(3), filter = LocationId("l2")), {})
        },
        "pantry_add_form" to {
            BottomSheetSurface(title = "Add to pantry") {
                AddItemContent(AddItemState(name = "Brown rice", amountText = "2"), content.locations, onEvent = {})
            }
        },
        "pantry_add_form_errors" to {
            BottomSheetSurface(title = "Add to pantry") {
                val form = AddItemState(
                    amountText = "lots",
                    nameError = PantryFieldError.REQUIRED,
                    amountError = PantryFieldError.INVALID,
                )
                AddItemContent(form, content.locations, onEvent = {})
            }
        },
        "pantry_item_details" to {
            BottomSheetSurface(title = items[1].name) {
                ItemDetailsContent(items[1], sheetDetails(), content, onEvent = {})
            }
        },
        "pantry_item_details_error" to {
            BottomSheetSurface(title = items[1].name) {
                ItemDetailsContent(items[1], sheetDetails(PantryError.OFFLINE), content, onEvent = {})
            }
        },
    )

    @Test
    fun everyScreenStateInLightAndDark() {
        screens.forEach { (name, screen) ->
            listOf(false, true).forEach { dark ->
                runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
                    setContent {
                        AppTheme(darkTheme = dark) {
                            Box(
                                Modifier.testTag(TAG).size(DpSize(AppTheme.spacing.xxl * 8, AppTheme.spacing.xxl * 14)),
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

    /** Design-plan 7.3 item 10: text at 200% must not clip; the key screens are captured at that size. */
    @Test
    fun keyScreensAtTwiceTheTextSize() {
        screens.filter { (name, _) -> name in largeTextScreens }.forEach { (name, screen) ->
            runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
                setContent {
                    AppTheme {
                        val density = Density(LocalDensity.current.density, fontScale = LARGE_FONT_SCALE)
                        CompositionLocalProvider(LocalDensity provides density) {
                            Box(
                                Modifier.testTag(TAG)
                                    .size(DpSize(AppTheme.spacing.xxl * 8, AppTheme.spacing.xxl * 14))
                                    .background(AppTheme.colors.surface),
                            ) {
                                screen()
                            }
                        }
                    }
                }
                onNodeWithTag(TAG).captureRoboImage("screenshots/${name}_large_text.png")
            }
        }
    }

    private companion object {
        const val LARGE_FONT_SCALE = 2f
        const val TAG = "screen"
        const val WIDTH = 420
        const val HEIGHT = 720
    }
}
