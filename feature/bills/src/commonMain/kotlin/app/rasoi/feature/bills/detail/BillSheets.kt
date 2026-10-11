package app.rasoi.feature.bills.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.rasoi.core.designsystem.component.AppBottomSheet
import app.rasoi.core.designsystem.component.AppButton
import app.rasoi.core.designsystem.component.AppText
import app.rasoi.core.designsystem.component.AppTextField
import app.rasoi.core.designsystem.component.ChoiceChips
import app.rasoi.core.designsystem.component.ChoiceOption
import app.rasoi.core.designsystem.component.MemberOption
import app.rasoi.core.designsystem.component.MemberPicker
import app.rasoi.core.designsystem.component.QuantityStepper
import app.rasoi.core.designsystem.component.SplitSelector
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.model.BillLine
import app.rasoi.core.model.ChargeDistribution
import app.rasoi.core.model.SplitMode
import app.rasoi.core.model.UserId
import app.rasoi.feature.bills.resources.Res
import app.rasoi.feature.bills.resources.changes_comment
import app.rasoi.feature.bills.resources.changes_send
import app.rasoi.feature.bills.resources.changes_title
import app.rasoi.feature.bills.resources.charge_equal
import app.rasoi.feature.bills.resources.charge_proportional
import app.rasoi.feature.bills.resources.charge_title
import app.rasoi.feature.bills.resources.payer_title
import app.rasoi.feature.bills.resources.split_error_members
import app.rasoi.feature.bills.resources.split_error_one
import app.rasoi.feature.bills.resources.split_error_units
import app.rasoi.feature.bills.resources.split_save
import app.rasoi.feature.bills.resources.split_title
import app.rasoi.feature.bills.resources.split_units_of
import app.rasoi.feature.bills.resources.split_who
import app.rasoi.feature.bills.text
import org.jetbrains.compose.resources.stringResource

/** The sheet that is open over the bill, if any. */
@Composable
internal fun BillSheets(state: BillDetailUiState.Content, onEvent: (BillDetailUiEvent) -> Unit) {
    val dismiss = { onEvent(BillDetailUiEvent.DismissSheet) }
    when (val sheet = state.sheet) {
        null -> Unit
        is BillSheet.Split -> state.bill.lines.firstOrNull { it.id == sheet.lineId }?.let { line ->
            AppBottomSheet(title = stringResource(Res.string.split_title, line.displayName), onDismiss = dismiss) {
                SplitContent(sheet, line, state, onEvent)
            }
        }
        BillSheet.Payer -> AppBottomSheet(title = stringResource(Res.string.payer_title), onDismiss = dismiss) {
            PayerContent(state, onEvent)
        }
        is BillSheet.Charge -> state.bill.charges.firstOrNull { it.id == sheet.chargeId }?.let { charge ->
            AppBottomSheet(title = stringResource(Res.string.charge_title, charge.label), onDismiss = dismiss) {
                ChargeContent(charge.distribution, state, onEvent)
            }
        }
        is BillSheet.RequestChanges -> AppBottomSheet(
            title = stringResource(Res.string.changes_title),
            onDismiss = dismiss,
        ) {
            RequestChangesContent(sheet, state, onEvent)
        }
    }
}

/** Who shares one item and how: one person, equally, or by the units each took. */
@Composable
fun SplitContent(
    sheet: BillSheet.Split,
    line: BillLine,
    state: BillDetailUiState.Content,
    onEvent: (BillDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        SplitSelector(selected = sheet.mode, onSelect = { onEvent(BillDetailUiEvent.Split.ModeSelected(it)) })
        AppText(stringResource(Res.string.split_who), style = AppTheme.type.label)
        MemberPicker(
            members = state.participants.map { MemberOption(it.userId.value, it.displayName) },
            selectedIds = sheet.units.keys.map { it.value }.toSet(),
            onToggle = { onEvent(BillDetailUiEvent.Split.MemberToggled(UserId(it))) },
        )
        if (sheet.mode == SplitMode.QUANTITY) UnitSteppers(sheet, line, state, onEvent)
        sheet.error?.let { error ->
            AppText(error.text(line.units), color = AppTheme.colors.danger)
        }
        AppButton(
            text = stringResource(Res.string.split_save),
            onClick = { onEvent(BillDetailUiEvent.Split.Save) },
            loading = state.isBusy,
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let { AppText(it.text(), color = AppTheme.colors.danger) }
    }
}

@Composable
private fun UnitSteppers(
    sheet: BillSheet.Split,
    line: BillLine,
    state: BillDetailUiState.Content,
    onEvent: (BillDetailUiEvent) -> Unit,
) {
    val names = state.names
    sheet.units.forEach { (user, units) ->
        val name = names[user].orEmpty()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(name)
            QuantityStepper(
                valueText = units.toString(),
                itemName = name,
                onDecrement = { onEvent(BillDetailUiEvent.Split.UnitsStepped(user, -1)) },
                onIncrement = { onEvent(BillDetailUiEvent.Split.UnitsStepped(user, 1)) },
                canDecrement = units > 1,
                canIncrement = units < line.units,
            )
        }
    }
    AppText(
        text = stringResource(Res.string.split_units_of, sheet.units.values.sum(), line.units),
        style = AppTheme.type.caption,
        color = AppTheme.colors.onSurfaceVariant,
    )
}

@Composable
private fun SplitError.text(units: Int): String = when (this) {
    SplitError.NO_MEMBERS -> stringResource(Res.string.split_error_members)
    SplitError.ONE_MEMBER -> stringResource(Res.string.split_error_one)
    SplitError.UNITS -> stringResource(Res.string.split_error_units, units)
}

/** Picks who paid the bill; only people who can owe a share are offered. */
@Composable
fun PayerContent(
    state: BillDetailUiState.Content,
    onEvent: (BillDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        ChoiceChips(
            options = state.participants.map { ChoiceOption(it.userId, it.displayName) },
            selected = state.bill.payerId,
            onSelect = { onEvent(BillDetailUiEvent.Change.PayerSelected(it)) },
        )
        state.error?.let { AppText(it.text(), color = AppTheme.colors.danger) }
    }
}

/** Shares a fee or discount in proportion to each person's items, or equally. */
@Composable
fun ChargeContent(
    current: ChargeDistribution,
    state: BillDetailUiState.Content,
    onEvent: (BillDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        ChoiceChips(
            options = listOf(
                ChoiceOption(ChargeDistribution.PROPORTIONAL, stringResource(Res.string.charge_proportional)),
                ChoiceOption(ChargeDistribution.EQUAL, stringResource(Res.string.charge_equal)),
            ),
            selected = current,
            onSelect = { onEvent(BillDetailUiEvent.Change.DistributionSelected(it)) },
        )
        state.error?.let { AppText(it.text(), color = AppTheme.colors.danger) }
    }
}

/** Asks for changes with a reason the uploader will see. */
@Composable
fun RequestChangesContent(
    sheet: BillSheet.RequestChanges,
    state: BillDetailUiState.Content,
    onEvent: (BillDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppTextField(
            value = sheet.comment,
            onValueChange = { onEvent(BillDetailUiEvent.Change.CommentChanged(it)) },
            label = stringResource(Res.string.changes_comment),
            error = sheet.commentError?.text(),
            modifier = Modifier.fillMaxWidth(),
        )
        AppButton(
            text = stringResource(Res.string.changes_send),
            onClick = { onEvent(BillDetailUiEvent.Change.SendChangeRequest) },
            loading = state.isBusy,
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let { AppText(it.text(), color = AppTheme.colors.danger) }
    }
}
