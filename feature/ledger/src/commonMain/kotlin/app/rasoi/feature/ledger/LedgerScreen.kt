package app.rasoi.feature.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.rasoi.core.designsystem.component.AmountText
import app.rasoi.core.designsystem.component.AmountTone
import app.rasoi.core.designsystem.component.AppBottomSheet
import app.rasoi.core.designsystem.component.AppButton
import app.rasoi.core.designsystem.component.AppButtonVariant
import app.rasoi.core.designsystem.component.AppText
import app.rasoi.core.designsystem.component.AppTextField
import app.rasoi.core.designsystem.component.AvatarSize
import app.rasoi.core.designsystem.component.ChoiceChips
import app.rasoi.core.designsystem.component.ChoiceOption
import app.rasoi.core.designsystem.component.ConfirmDialog
import app.rasoi.core.designsystem.component.ErrorView
import app.rasoi.core.designsystem.component.LoadingView
import app.rasoi.core.designsystem.component.MemberAvatar
import app.rasoi.core.designsystem.component.NoticeBanner
import app.rasoi.core.designsystem.component.NoticeTone
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.LedgerTransfer
import app.rasoi.core.model.Money
import app.rasoi.core.model.PeriodMonth
import app.rasoi.core.model.PeriodStatus
import app.rasoi.core.model.Role
import app.rasoi.core.model.SettlementStatus
import app.rasoi.core.model.UserId
import app.rasoi.feature.ledger.resources.Res
import app.rasoi.feature.ledger.resources.ledger_all_settled
import app.rasoi.feature.ledger.resources.ledger_balances
import app.rasoi.feature.ledger.resources.ledger_close
import app.rasoi.feature.ledger.resources.ledger_close_confirm
import app.rasoi.feature.ledger.resources.ledger_close_message
import app.rasoi.feature.ledger.resources.ledger_close_title
import app.rasoi.feature.ledger.resources.ledger_edit_upi
import app.rasoi.feature.ledger.resources.ledger_load_error
import app.rasoi.feature.ledger.resources.ledger_mark_paid
import app.rasoi.feature.ledger.resources.ledger_my_upi
import app.rasoi.feature.ledger.resources.ledger_my_upi_none
import app.rasoi.feature.ledger.resources.ledger_paid
import app.rasoi.feature.ledger.resources.ledger_pay_upi
import app.rasoi.feature.ledger.resources.ledger_pays
import app.rasoi.feature.ledger.resources.ledger_pending_bill_message
import app.rasoi.feature.ledger.resources.ledger_pending_bills_message
import app.rasoi.feature.ledger.resources.ledger_pending_bills_title
import app.rasoi.feature.ledger.resources.ledger_period_closed
import app.rasoi.feature.ledger.resources.ledger_period_open
import app.rasoi.feature.ledger.resources.ledger_set_upi
import app.rasoi.feature.ledger.resources.ledger_suggested
import app.rasoi.feature.ledger.resources.ledger_transfers
import app.rasoi.feature.ledger.resources.upi_hint
import app.rasoi.feature.ledger.resources.upi_invalid
import app.rasoi.feature.ledger.resources.upi_label
import app.rasoi.feature.ledger.resources.upi_save
import app.rasoi.feature.ledger.resources.upi_title
import app.rasoi.feature.ledger.upi.rememberUpiLauncher
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The screen's ViewModel, keyed per [key]; the app creates all tab ViewModels up front so tabs open already loaded. */
@Composable
fun ledgerViewModel(
    householdId: HouseholdId,
    myUserId: UserId,
    myRole: Role,
    key: String = householdId.value,
): LedgerViewModel = koinViewModel(key = key) { parametersOf(householdId, myUserId, myRole) }

@Composable
fun LedgerRoute(
    householdId: HouseholdId,
    myUserId: UserId,
    myRole: Role,
    modifier: Modifier = Modifier,
    viewModel: LedgerViewModel = ledgerViewModel(householdId, myUserId, myRole),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val launcher = rememberUpiLauncher()
    LaunchedEffect(viewModel) {
        viewModel.onEvent(LedgerUiEvent.Refresh)
        viewModel.effects.collect { effect ->
            when (effect) {
                is LedgerEffect.OpenUpi -> if (!launcher.open(
                        effect.link,
                    )
                ) {
                    viewModel.onEvent(LedgerUiEvent.Upi.Unavailable)
                }
            }
        }
    }
    LedgerScreen(state = state, onEvent = viewModel::onEvent, modifier = modifier)
}

/** Balances for a month, the transfers that settle them, and the owner's way to close the month. */
@Composable
fun LedgerScreen(state: LedgerUiState, onEvent: (LedgerUiEvent) -> Unit, modifier: Modifier = Modifier) {
    when (state) {
        LedgerUiState.Loading -> LoadingView(modifier)
        is LedgerUiState.Error -> ErrorView(
            message = stringResource(Res.string.ledger_load_error, state.error.text()),
            onRetry = { onEvent(LedgerUiEvent.Retry) },
            modifier = modifier,
        )
        is LedgerUiState.Content -> {
            LedgerContent(state, onEvent, modifier)
            LedgerSheets(state, onEvent)
        }
    }
}

@Composable
private fun LedgerContent(state: LedgerUiState.Content, onEvent: (LedgerUiEvent) -> Unit, modifier: Modifier) {
    val ledger = state.ledger
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppTheme.spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        if (ledger.months.size > 1) item { MonthChips(state, onEvent) }
        item { PeriodTitle(ledger.month, ledger.status) }
        if (ledger.pendingBills > 0) item { PendingNotice(ledger.pendingBills) }
        if (state.sheet ==
            null
        ) {
            state.error?.let { error -> item { AppText(error.text(), color = AppTheme.colors.danger) } }
        }
        item { SectionTitle(stringResource(Res.string.ledger_balances)) }
        ledger.balances.forEach { balance ->
            item(key = "balance-${balance.userId.value}") {
                PersonRow(state.names[balance.userId].orEmpty()) {
                    AmountText(amount = balance.net.absoluteValue, tone = balance.net.tone())
                }
            }
        }
        item { SectionTitle(stringResource(Res.string.ledger_transfers)) }
        if (ledger.transfers.isEmpty()) {
            item { AppText(stringResource(Res.string.ledger_all_settled), color = AppTheme.colors.onSurfaceVariant) }
        } else if (ledger.status == PeriodStatus.OPEN) {
            item {
                AppText(
                    stringResource(Res.string.ledger_suggested),
                    style = AppTheme.type.caption,
                    color = AppTheme.colors.onSurfaceVariant,
                )
            }
        }
        ledger.transfers.forEachIndexed { index, transfer ->
            item(key = "transfer-$index") { TransferCard(transfer, state, onEvent) }
        }
        item { UpiRow(state, onEvent) }
        if (state.canClose) {
            item {
                AppButton(
                    text = stringResource(Res.string.ledger_close),
                    onClick = { onEvent(LedgerUiEvent.AskClose) },
                    variant = AppButtonVariant.SECONDARY,
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun Money.tone() = when {
    isZero -> AmountTone.NEUTRAL
    isNegative -> AmountTone.YOU_OWE
    else -> AmountTone.OWED_TO_YOU
}

@Composable
private fun MonthChips(state: LedgerUiState.Content, onEvent: (LedgerUiEvent) -> Unit) {
    ChoiceChips(
        options = state.ledger.months.map { ChoiceOption(it, it.toString()) },
        selected = state.ledger.month,
        onSelect = { onEvent(LedgerUiEvent.SelectMonth(it)) },
    )
}

@Composable
private fun PeriodTitle(month: PeriodMonth, status: PeriodStatus) {
    val label = when (status) {
        PeriodStatus.OPEN -> stringResource(Res.string.ledger_period_open, month.toString())
        PeriodStatus.LOCKED -> stringResource(Res.string.ledger_period_closed, month.toString())
    }
    AppText(label, style = AppTheme.type.title, heading = true)
}

@Composable
private fun PendingNotice(count: Int) {
    NoticeBanner(
        tone = NoticeTone.INFO,
        title = stringResource(Res.string.ledger_pending_bills_title),
        message = if (count == 1) {
            stringResource(Res.string.ledger_pending_bill_message)
        } else {
            stringResource(Res.string.ledger_pending_bills_message, count)
        },
    )
}

@Composable
private fun SectionTitle(text: String) {
    AppText(text, style = AppTheme.type.label, color = AppTheme.colors.onSurfaceVariant, heading = true)
}

@Composable
private fun PersonRow(name: String, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = AppTheme.spacing.minTouchTarget),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MemberAvatar(name = name, size = AvatarSize.SMALL)
        AppText(name, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun TransferCard(item: LedgerTransfer, state: LedgerUiState.Content, onEvent: (LedgerUiEvent) -> Unit) {
    val from = state.names[item.transfer.from].orEmpty()
    val to = state.names[item.transfer.to].orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
        PersonRow(stringResource(Res.string.ledger_pays, from, to)) {
            AmountText(amount = item.transfer.amount)
        }
        if (item.status == SettlementStatus.PAID) {
            AppText(
                stringResource(Res.string.ledger_paid),
                style = AppTheme.type.caption,
                color = AppTheme.colors.success,
            )
        }
        val settlementId = item.settlementId
        if (settlementId != null && state.canPayByUpi(item)) {
            AppButton(
                text = stringResource(Res.string.ledger_pay_upi),
                onClick = { onEvent(LedgerUiEvent.PayWithUpi(settlementId)) },
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (settlementId != null && state.canMarkPaid(item)) {
            AppButton(
                text = stringResource(Res.string.ledger_mark_paid),
                onClick = { onEvent(LedgerUiEvent.MarkPaid(settlementId)) },
                variant = AppButtonVariant.SECONDARY,
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun UpiRow(state: LedgerUiState.Content, onEvent: (LedgerUiEvent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = AppTheme.spacing.minTouchTarget),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AppText(stringResource(Res.string.ledger_my_upi), style = AppTheme.type.label)
            AppText(
                text = state.myUpi ?: stringResource(Res.string.ledger_my_upi_none),
                style = AppTheme.type.caption,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
        AppButton(
            text = stringResource(if (state.myUpi == null) Res.string.ledger_set_upi else Res.string.ledger_edit_upi),
            onClick = { onEvent(LedgerUiEvent.Upi.Edit) },
            variant = AppButtonVariant.TEXT,
        )
    }
}

@Composable
private fun LedgerSheets(state: LedgerUiState.Content, onEvent: (LedgerUiEvent) -> Unit) {
    when (val sheet = state.sheet) {
        null -> Unit
        LedgerSheet.ConfirmClose -> ConfirmDialog(
            title = stringResource(Res.string.ledger_close_title, state.ledger.month.toString()),
            message = stringResource(Res.string.ledger_close_message),
            confirmLabel = stringResource(Res.string.ledger_close_confirm),
            destructive = false,
            onConfirm = { onEvent(LedgerUiEvent.ConfirmClose) },
            onDismiss = { onEvent(LedgerUiEvent.DismissSheet) },
        )
        is LedgerSheet.Upi -> AppBottomSheet(
            title = stringResource(Res.string.upi_title),
            onDismiss = { onEvent(LedgerUiEvent.DismissSheet) },
        ) { UpiContent(sheet, state, onEvent) }
    }
}

/** Edits the viewer's own UPI address. */
@Composable
fun UpiContent(
    sheet: LedgerSheet.Upi,
    state: LedgerUiState.Content,
    onEvent: (LedgerUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppTextField(
            value = sheet.text,
            onValueChange = { onEvent(LedgerUiEvent.Upi.Changed(it)) },
            label = stringResource(Res.string.upi_label),
            error = if (sheet.invalid) stringResource(Res.string.upi_invalid) else null,
            supportingText = stringResource(Res.string.upi_hint),
            modifier = Modifier.fillMaxWidth(),
        )
        AppButton(
            text = stringResource(Res.string.upi_save),
            onClick = { onEvent(LedgerUiEvent.Upi.Save) },
            loading = state.isBusy,
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let { AppText(it.text(), color = AppTheme.colors.danger) }
    }
}
