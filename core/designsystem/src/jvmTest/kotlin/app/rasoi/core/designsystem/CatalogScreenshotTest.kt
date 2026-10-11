package app.rasoi.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import app.rasoi.core.designsystem.catalog.catalogEntries
import app.rasoi.core.designsystem.theme.AppTheme
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class CatalogScreenshotTest {
    @Test
    fun everyCatalogEntryInLightAndDark() {
        catalogEntries.forEach { entry ->
            listOf(false, true).forEach { dark ->
                runDesktopComposeUiTest(width = WINDOW_WIDTH, height = WINDOW_HEIGHT) {
                    setContent {
                        AppTheme(darkTheme = dark) {
                            Box(
                                Modifier
                                    .testTag(TAG)
                                    .width(AppTheme.spacing.xxl * CONTENT_WIDTH_UNITS)
                                    .background(AppTheme.colors.surface)
                                    .padding(AppTheme.spacing.md),
                            ) { entry.content() }
                        }
                    }
                    onNodeWithTag(TAG).captureRoboImage("screenshots/${entry.id}_${if (dark) "dark" else "light"}.png")
                }
            }
        }
    }

    private companion object {
        const val TAG = "catalog-entry"
        const val WINDOW_WIDTH = 480
        const val WINDOW_HEIGHT = 720
        const val CONTENT_WIDTH_UNITS = 8
    }
}
