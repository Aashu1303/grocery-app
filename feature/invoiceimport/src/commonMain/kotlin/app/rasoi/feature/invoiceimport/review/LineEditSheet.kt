package app.rasoi.feature.invoiceimport.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.rasoi.core.designsystem.component.AppButton
import app.rasoi.core.designsystem.component.AppKeyboard
import app.rasoi.core.designsystem.component.AppText
import app.rasoi.core.designsystem.component.AppTextField
import app.rasoi.core.designsystem.component.ChoiceChips
import app.rasoi.core.designsystem.component.ChoiceOption
import app.rasoi.core.designsystem.component.QuantityStepper
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.domain.bills.BillRules
import app.rasoi.feature.invoiceimport.resources.Res
import app.rasoi.feature.invoiceimport.resources.field_invalid_amount
import app.rasoi.feature.invoiceimport.resources.field_required
import app.rasoi.feature.invoiceimport.resources.field_too_long
import app.rasoi.feature.invoiceimport.resources.review_edit_amount
import app.rasoi.feature.invoiceimport.resources.review_edit_kind
import app.rasoi.feature.invoiceimport.resources.review_edit_name
import app.rasoi.feature.invoiceimport.resources.review_edit_save
import app.rasoi.feature.invoiceimport.resources.review_edit_units
import app.rasoi.feature.invoiceimport.resources.review_kind_food
import app.rasoi.feature.invoiceimport.resources.review_kind_other
import app.rasoi.feature.invoiceimport.text
import org.jetbrains.compose.resources.stringResource

/** Name, quantity and amount paid for one bill item. */
@Composable
fun LineEditContent(state: LineEditState, onEvent: (BillReviewUiEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppTextField(
            value = state.name,
            onValueChange = { onEvent(BillReviewUiEvent.NameChanged(it)) },
            label = stringResource(Res.string.review_edit_name),
            error = state.nameError?.text(),
            modifier = Modifier.fillMaxWidth(),
        )
        AppTextField(
            value = state.amountText,
            onValueChange = { onEvent(BillReviewUiEvent.AmountChanged(it)) },
            label = stringResource(Res.string.review_edit_amount),
            error = state.amountError?.text(),
            keyboard = AppKeyboard.DECIMAL,
            modifier = Modifier.fillMaxWidth(),
        )
        AppText(stringResource(Res.string.review_edit_kind), style = AppTheme.type.label)
        ChoiceChips(
            options = listOf(
                ChoiceOption(true, stringResource(Res.string.review_kind_food)),
                ChoiceOption(false, stringResource(Res.string.review_kind_other)),
            ),
            selected = state.isFood,
            onSelect = { onEvent(BillReviewUiEvent.FoodChanged(it)) },
        )
        AppText(stringResource(Res.string.review_edit_units), style = AppTheme.type.label)
        QuantityStepper(
            valueText = state.units.toString(),
            itemName = state.name,
            onDecrement = { onEvent(BillReviewUiEvent.UnitsStepped(-1)) },
            onIncrement = { onEvent(BillReviewUiEvent.UnitsStepped(1)) },
            canDecrement = state.units > 1,
            canIncrement = state.units < BillRules.MAX_UNITS,
        )
        AppButton(
            text = stringResource(Res.string.review_edit_save),
            onClick = { onEvent(BillReviewUiEvent.SaveLine) },
            loading = state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let { AppText(it.text(), color = AppTheme.colors.danger) }
    }
}

@Composable
private fun LineFieldError.text(): String = stringResource(
    when (this) {
        LineFieldError.REQUIRED -> Res.string.field_required
        LineFieldError.TOO_LONG -> Res.string.field_too_long
        LineFieldError.INVALID -> Res.string.field_invalid_amount
    },
)
