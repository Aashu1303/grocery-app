package app.rasoi.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.model.Money

/** Bill line: name, how it is split, an optional tag such as "Non-food", and the amount paid. */
@Composable
fun BillLineRow(
    name: String,
    amount: Money,
    allocationSummary: String,
    modifier: Modifier = Modifier,
    tag: String? = null,
    onClick: (() -> Unit)? = null,
) {
    ListRow(
        modifier = modifier,
        onClick = onClick,
        leading = {
            Text(text = name, style = AppTheme.type.body, color = AppTheme.colors.onSurface)
            Text(text = allocationSummary, style = AppTheme.type.caption, color = AppTheme.colors.onSurfaceVariant)
            if (tag != null) Text(text = tag, style = AppTheme.type.caption, color = AppTheme.colors.primary)
        },
        trailing = { AmountText(amount = amount) },
    )
}
