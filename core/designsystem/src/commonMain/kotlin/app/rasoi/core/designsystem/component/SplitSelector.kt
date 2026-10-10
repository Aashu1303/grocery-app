package app.rasoi.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.rasoi.core.designsystem.resources.Res
import app.rasoi.core.designsystem.resources.ds_split_equal
import app.rasoi.core.designsystem.resources.ds_split_one
import app.rasoi.core.designsystem.resources.ds_split_quantity
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.model.SplitMode
import org.jetbrains.compose.resources.stringResource

/** Chooses how one bill line is shared: one person, equally, or by quantity. */
@Composable
fun SplitSelector(selected: SplitMode, onSelect: (SplitMode) -> Unit, modifier: Modifier = Modifier) {
    val modes = SplitMode.entries
    val colors = AppTheme.colors
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                modifier = Modifier.heightIn(min = AppTheme.spacing.minTouchTarget),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = colors.primaryContainer,
                    activeContentColor = colors.onPrimaryContainer,
                    activeBorderColor = colors.outline,
                    inactiveContainerColor = colors.surface,
                    inactiveContentColor = colors.onSurface,
                    inactiveBorderColor = colors.outline,
                ),
                label = { Text(text = mode.label(), style = AppTheme.type.label) },
            )
        }
    }
}

@Composable
private fun SplitMode.label(): String = stringResource(
    when (this) {
        SplitMode.ONE -> Res.string.ds_split_one
        SplitMode.EQUAL -> Res.string.ds_split_equal
        SplitMode.QUANTITY -> Res.string.ds_split_quantity
    },
)
