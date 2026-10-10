package app.rasoi.core.designsystem.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/** One component in one state. The catalog screen and the screenshot tests render the same list. */
@Immutable
class CatalogEntry(val component: String, val state: String, val content: @Composable () -> Unit) {
    val id: String get() = "${component}_$state"
}
