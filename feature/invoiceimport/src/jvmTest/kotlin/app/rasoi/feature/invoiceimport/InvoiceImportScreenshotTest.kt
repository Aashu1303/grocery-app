package app.rasoi.feature.invoiceimport

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
import app.rasoi.core.domain.bills.InvoiceFile
import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.ImportConfidence
import app.rasoi.core.model.Money
import app.rasoi.core.model.Platform
import app.rasoi.core.model.Quantity
import app.rasoi.core.testing.Builders
import app.rasoi.feature.invoiceimport.review.BillReviewScreen
import app.rasoi.feature.invoiceimport.review.BillReviewUiState
import app.rasoi.feature.invoiceimport.review.LineEditContent
import app.rasoi.feature.invoiceimport.review.LineEditState
import app.rasoi.feature.invoiceimport.review.LineFieldError
import app.rasoi.feature.invoiceimport.upload.ImportScreen
import app.rasoi.feature.invoiceimport.upload.ImportUiState
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

/** Every import and review state, light and dark (design-plan 7.3: loading, empty, error, content). */
@OptIn(ExperimentalTestApi::class)
class InvoiceImportScreenshotTest {
    private val pdfs = listOf(
        InvoiceFile("zepto-invoice-10-oct.pdf", ByteArray(184 * 1024)),
        InvoiceFile("zepto-fees.pdf", ByteArray(41 * 1024)),
    )

    private val lines = listOf(
        Builders.line("l1", "Bottle Gourd-Long | Small", 1_900).copy(measure = Quantity.grams(470)),
        Builders.line("l2", "Beans Lobia", 2_800).copy(measure = Quantity.grams(360)),
        Builders.line("l3", "Amul Taaza Toned Milk", 6_600, units = 2),
    )
    private val charges = listOf(
        Builders.charge("c1", ChargeKind.DELIVERY, "Delivery Fee", 1_900),
        Builders.charge("c2", ChargeKind.DISCOUNT, "Delivery Discount", -1_000),
    )
    private val bill = Builders.bill(lines = lines, charges = charges).copy(platform = Platform.HANDPICKD)
    private val mismatch = bill.copy(total = Money(13_100))
    private val fallback = bill.copy(confidence = ImportConfidence.LOW)
    private val edit = LineEditState(BillLineId("l2"), "Beans Lobia", 1, "28.00", isFood = true)

    private val largeTextScreens = setOf("import_files", "review_mismatch", "edit_line")

    private val screens: List<Pair<String, @Composable () -> Unit>> = listOf(
        "import_empty" to { ImportScreen(ImportUiState(), {}, {}, {}) },
        "import_empty_unsupported" to { ImportScreen(ImportUiState(error = BillError.UNSUPPORTED), {}, {}, {}) },
        "import_files" to { ImportScreen(ImportUiState(files = pdfs), {}, {}, {}) },
        "import_uploading" to { ImportScreen(ImportUiState(files = pdfs, isUploading = true), {}, {}, {}) },
        "import_duplicate" to { ImportScreen(ImportUiState(files = pdfs, error = BillError.DUPLICATE), {}, {}, {}) },
        "review_loading" to { BillReviewScreen(BillReviewUiState.Loading, {}, {}) },
        "review_error" to { BillReviewScreen(BillReviewUiState.Error(BillError.OFFLINE), {}, {}) },
        "review_clean" to { BillReviewScreen(BillReviewUiState.Content(bill), {}, {}) },
        "review_mismatch" to { BillReviewScreen(BillReviewUiState.Content(mismatch), {}, {}) },
        "review_fallback" to { BillReviewScreen(BillReviewUiState.Content(fallback), {}, {}) },
        "review_non_food" to {
            val pack = lines.last().copy(displayName = "Reusable hot / cold pack", isFood = false)
            BillReviewScreen(BillReviewUiState.Content(bill.copy(lines = lines.dropLast(1) + pack)), {}, {})
        },
        "review_save_failed" to {
            BillReviewScreen(BillReviewUiState.Content(mismatch, error = BillError.OFFLINE), {}, {})
        },
        "edit_line" to {
            BottomSheetSurface(title = "Edit item") { LineEditContent(edit, onEvent = {}) }
        },
        "edit_line_errors" to {
            BottomSheetSurface(title = "Edit item") {
                LineEditContent(
                    edit.copy(
                        name = "",
                        amountText = "abc",
                        nameError = LineFieldError.REQUIRED,
                        amountError = LineFieldError.INVALID,
                    ),
                    onEvent = {},
                )
            }
        },
    )

    @Test
    fun everyScreenStateInLightAndDark() {
        screens.forEach { (name, content) ->
            listOf(false, true).forEach { dark ->
                runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
                    setContent {
                        AppTheme(darkTheme = dark) {
                            Box(
                                Modifier.testTag(TAG).size(DpSize(AppTheme.spacing.xxl * 8, AppTheme.spacing.xxl * 14)),
                            ) {
                                content()
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
