package app.rasoi.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.rasoi.core.designsystem.theme.AppTheme

/** Modal picker sheet with a title. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = AppTheme.shapes.sheet,
        containerColor = AppTheme.colors.surface,
        contentColor = AppTheme.colors.onSurface,
        tonalElevation = AppTheme.shapes.elevationHigh,
    ) { SheetContent(title, content) }
}

/** The sheet body on its own, for previews, the catalog and screenshot tests. */
@Composable
fun BottomSheetSurface(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = AppTheme.shapes.sheet,
        color = AppTheme.colors.surface,
        contentColor = AppTheme.colors.onSurface,
        shadowElevation = AppTheme.shapes.elevationHigh,
    ) { SheetContent(title, content) }
}

@Composable
private fun SheetContent(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(AppTheme.spacing.md)) {
        Text(text = title, style = AppTheme.type.title, modifier = Modifier.semantics { heading() })
        content()
    }
}
