package app.rasoi.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import app.rasoi.core.designsystem.resources.Res
import app.rasoi.core.designsystem.resources.ds_owed_to_you
import app.rasoi.core.designsystem.resources.ds_you_owe
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.model.Money
import org.jetbrains.compose.resources.stringResource

/**
 * A rupee amount in Indian grouping with tabular figures, right-aligned. Owed amounts carry a sign, a colour and
 * a word, so meaning never depends on colour alone.
 */
@Composable
fun AmountText(
    amount: Money,
    modifier: Modifier = Modifier,
    tone: AmountTone = AmountTone.NEUTRAL,
    style: TextStyle = AppTheme.type.numeric,
) {
    val colors = AppTheme.colors
    val (sign, color, label) = when (tone) {
        AmountTone.NEUTRAL -> Triple("", colors.onSurface, null)
        AmountTone.OWED_TO_YOU -> Triple("+", colors.owedToYou, stringResource(Res.string.ds_owed_to_you))
        AmountTone.YOU_OWE -> Triple("−", colors.youOwe, stringResource(Res.string.ds_you_owe))
    }
    val text = if (tone == AmountTone.NEUTRAL) amount.format() else sign + amount.absoluteValue.format()
    val spoken = listOfNotNull(text, label).joinToString(" ")
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = style, color = color, textAlign = TextAlign.End, softWrap = false)
        if (label != null) Text(text = label, style = AppTheme.type.caption, color = color)
    }
}
