package app.rasoi.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.rasoi.core.designsystem.theme.AppTheme

/** Modal picker sheet with a title. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    modifier: Modifier = Modifier,
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val bottomSheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.sheet,
        containerColor = AppTheme.colors.surface,
        contentColor = AppTheme.colors.onSurface,
        tonalElevation = AppTheme.shapes.elevationHigh,
        sheetState = bottomSheetState,
    ) {
        // Only the modal scrolls: it has a bounded height. The bare surface below is laid out in lists (the catalog),
        // where a scroll container would get an unbounded height and crash.
        SheetContent(
            title = title,
            modifier = Modifier
                .wrapContentHeight() // Hug the items tightly
                .verticalScroll(rememberScrollState()), // Only scrolls if content exceeds safety bounds
            content = content,
        )
    }
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
    ) { SheetContent(title, content = content) }
}

@Composable
private fun SheetContent(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .padding(AppTheme.spacing.md),
    ) {
        Text(
            text = title,
            style = AppTheme.type.title,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}
