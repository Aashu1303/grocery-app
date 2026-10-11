package app.rasoi.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.rasoi.core.designsystem.icon.AppIcons
import app.rasoi.core.designsystem.resources.Res
import app.rasoi.core.designsystem.resources.ds_decrease
import app.rasoi.core.designsystem.resources.ds_increase
import app.rasoi.core.designsystem.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Minus / value / plus control. [itemName] makes the button descriptions specific ("Increase Milk"). */
@Composable
fun QuantityStepper(
    valueText: String,
    itemName: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    canDecrement: Boolean = true,
    canIncrement: Boolean = true,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(AppIcons.Remove, stringResource(Res.string.ds_decrease, itemName), canDecrement, onDecrement)
        Text(text = valueText, style = AppTheme.type.numeric, color = AppTheme.colors.onSurface)
        StepButton(AppIcons.Add, stringResource(Res.string.ds_increase, itemName), canIncrement, onIncrement)
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(AppTheme.spacing.minTouchTarget),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = AppTheme.colors.primaryContainer,
            contentColor = AppTheme.colors.onPrimaryContainer,
            disabledContainerColor = AppTheme.colors.surfaceVariant,
            disabledContentColor = AppTheme.colors.onSurfaceVariant,
        ),
    ) {
        Icon(imageVector = icon, contentDescription = description)
    }
}
