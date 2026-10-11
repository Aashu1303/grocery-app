package app.rasoi.feature.pantry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.rasoi.core.designsystem.component.AppButton
import app.rasoi.core.designsystem.component.AppButtonVariant
import app.rasoi.core.designsystem.component.AppKeyboard
import app.rasoi.core.designsystem.component.AppText
import app.rasoi.core.designsystem.component.AppTextField
import app.rasoi.core.designsystem.component.ChoiceChips
import app.rasoi.core.designsystem.component.ChoiceOption
import app.rasoi.core.designsystem.component.QuantityStepper
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.domain.pantry.InputUnit
import app.rasoi.core.model.Location
import app.rasoi.core.model.LocationId
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.format
import app.rasoi.feature.pantry.resources.Res
import app.rasoi.feature.pantry.resources.pantry_amount
import app.rasoi.feature.pantry.resources.pantry_best_before
import app.rasoi.feature.pantry.resources.pantry_discard
import app.rasoi.feature.pantry.resources.pantry_finish
import app.rasoi.feature.pantry.resources.pantry_move_to
import app.rasoi.feature.pantry.resources.pantry_name
import app.rasoi.feature.pantry.resources.pantry_no_date
import app.rasoi.feature.pantry.resources.pantry_place
import app.rasoi.feature.pantry.resources.pantry_save
import org.jetbrains.compose.resources.stringResource

/** Name, quantity with a unit, and a storage place for a new item. */
@Composable
fun AddItemContent(
    form: AddItemState,
    locations: List<Location>,
    onEvent: (PantryUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppTextField(
            value = form.name,
            onValueChange = { onEvent(PantryUiEvent.Form.NameChanged(it)) },
            label = stringResource(Res.string.pantry_name),
            error = form.nameError?.text(),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm), verticalAlignment = Alignment.Top) {
            AppTextField(
                value = form.amountText,
                onValueChange = { onEvent(PantryUiEvent.Form.AmountChanged(it)) },
                label = stringResource(Res.string.pantry_amount),
                error = form.amountError?.text(),
                keyboard = AppKeyboard.DECIMAL,
                modifier = Modifier.weight(1f),
            )
        }
        ChoiceChips(
            options = InputUnit.entries.map { ChoiceOption(it, it.label()) },
            selected = form.unit,
            onSelect = { onEvent(PantryUiEvent.Form.UnitSelected(it)) },
        )
        AppText(stringResource(Res.string.pantry_place), style = AppTheme.type.label)
        ChoiceChips(
            options = locations.map { ChoiceOption<LocationId?>(it.id, it.name) },
            selected = form.locationId,
            onSelect = { id -> id?.let { onEvent(PantryUiEvent.Form.LocationSelected(it)) } },
        )
        AppButton(
            text = stringResource(Res.string.pantry_save),
            onClick = { onEvent(PantryUiEvent.SaveNew) },
            loading = form.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        form.error?.let { AppText(it.text(), color = AppTheme.colors.danger) }
    }
}

/** Move an item, adjust its best-before date by days, mark it finished or discard it. */
@Composable
fun ItemDetailsContent(
    item: PantryItem,
    sheet: PantrySheet.Details,
    state: PantryUiState.Content,
    onEvent: (PantryUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppText(item.quantity.format(), color = AppTheme.colors.onSurfaceVariant)
        AppText(stringResource(Res.string.pantry_move_to), style = AppTheme.type.label)
        ChoiceChips(
            options = state.locations.map { ChoiceOption<LocationId?>(it.id, it.name) },
            selected = item.locationId,
            onSelect = { id -> id?.let { onEvent(PantryUiEvent.Change.MoveTo(it)) } },
        )
        AppText(stringResource(Res.string.pantry_best_before), style = AppTheme.type.label)
        QuantityStepper(
            valueText = item.expiresOn?.shortText() ?: stringResource(Res.string.pantry_no_date),
            itemName = item.name,
            onDecrement = { onEvent(PantryUiEvent.Change.ExpiryStepped(-1)) },
            onIncrement = { onEvent(PantryUiEvent.Change.ExpiryStepped(1)) },
        )
        AppButton(
            text = stringResource(Res.string.pantry_finish),
            onClick = { onEvent(PantryUiEvent.Change.MarkFinished) },
            loading = sheet.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        AppButton(
            text = stringResource(Res.string.pantry_discard),
            onClick = { onEvent(PantryUiEvent.Change.AskDiscard) },
            variant = AppButtonVariant.DESTRUCTIVE,
            enabled = !sheet.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        sheet.error?.let { AppText(it.text(), color = AppTheme.colors.danger) }
    }
}
