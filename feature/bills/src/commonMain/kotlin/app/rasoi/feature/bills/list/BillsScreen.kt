package app.rasoi.feature.bills.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.rasoi.core.designsystem.component.AmountText
import app.rasoi.core.designsystem.component.AppBottomSheet
import app.rasoi.core.designsystem.component.AppButton
import app.rasoi.core.designsystem.component.AppButtonVariant
import app.rasoi.core.designsystem.component.AppCard
import app.rasoi.core.designsystem.component.AppKeyboard
import app.rasoi.core.designsystem.component.AppText
import app.rasoi.core.designsystem.component.AppTextField
import app.rasoi.core.designsystem.component.EmptyView
import app.rasoi.core.designsystem.component.ErrorView
import app.rasoi.core.designsystem.component.LoadingView
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillSummary
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Role
import app.rasoi.feature.bills.label
import app.rasoi.feature.bills.resources.Res
import app.rasoi.feature.bills.resources.bills_add
import app.rasoi.feature.bills.resources.bills_empty_message
import app.rasoi.feature.bills.resources.bills_empty_title
import app.rasoi.feature.bills.resources.bills_import
import app.rasoi.feature.bills.resources.bills_line_one
import app.rasoi.feature.bills.resources.bills_lines_count
import app.rasoi.feature.bills.resources.bills_load_error
import app.rasoi.feature.bills.resources.bills_new_amount
import app.rasoi.feature.bills.resources.bills_new_name
import app.rasoi.feature.bills.resources.bills_new_save
import app.rasoi.feature.bills.resources.bills_new_title
import app.rasoi.feature.bills.resources.bills_paid_by
import app.rasoi.feature.bills.text
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The screen's ViewModel, keyed per [key]; the app creates all tab ViewModels up front so tabs open already loaded. */
@Composable
fun billsViewModel(householdId: HouseholdId, myRole: Role, key: String = householdId.value): BillsViewModel =
    koinViewModel(key = key) { parametersOf(householdId, myRole) }

@Composable
fun BillsRoute(
    householdId: HouseholdId,
    myRole: Role,
    onOpenBill: (BillId) -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BillsViewModel = billsViewModel(householdId, myRole),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.onEvent(BillsUiEvent.Refresh)
        viewModel.effects.collect { effect ->
            when (effect) {
                is BillsEffect.OpenBill -> onOpenBill(effect.billId)
                BillsEffect.OpenImport -> onImport()
            }
        }
    }
    BillsScreen(state = state, onEvent = viewModel::onEvent, modifier = modifier)
}

/** The household's bills with a status on each, plus the ways to add one. */
@Composable
fun BillsScreen(state: BillsUiState, onEvent: (BillsUiEvent) -> Unit, modifier: Modifier = Modifier) {
    when (state) {
        BillsUiState.Loading -> LoadingView(modifier)
        is BillsUiState.Error -> ErrorView(
            message = stringResource(Res.string.bills_load_error, state.error.text()),
            onRetry = { onEvent(BillsUiEvent.Retry) },
            modifier = modifier,
        )
        is BillsUiState.Content -> {
            BillsContent(state, onEvent, modifier)
            state.form?.let { form ->
                AppBottomSheet(
                    title = stringResource(Res.string.bills_new_title),
                    onDismiss = { onEvent(BillsUiEvent.DismissNew) },
                ) { NewBillContent(form, onEvent) }
            }
        }
    }
}

@Composable
private fun BillsContent(state: BillsUiState.Content, onEvent: (BillsUiEvent) -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize()) {
        if (state.bills.isEmpty()) {
            EmptyView(
                title = stringResource(Res.string.bills_empty_title),
                message = stringResource(Res.string.bills_empty_message),
                modifier = Modifier.weight(1f).padding(AppTheme.spacing.screenPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(AppTheme.spacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                items(state.bills, key = { it.id.value }) { bill ->
                    BillCard(bill, state.names[bill.payerId].orEmpty()) { onEvent(BillsUiEvent.OpenBill(bill.id)) }
                }
            }
        }
        if (state.canEdit) AddActions(onEvent)
    }
}

@Composable
private fun AddActions(onEvent: (BillsUiEvent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        AppButton(
            text = stringResource(Res.string.bills_import),
            onClick = { onEvent(BillsUiEvent.ImportInvoice) },
            modifier = Modifier.fillMaxWidth(),
        )
        AppButton(
            text = stringResource(Res.string.bills_add),
            onClick = { onEvent(BillsUiEvent.OpenNew) },
            variant = AppButtonVariant.SECONDARY,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BillCard(bill: BillSummary, payer: String, onClick: () -> Unit) {
    AppCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        ) {
            AppText(
                text = bill.title ?: bill.platform.label(),
                style = AppTheme.type.title,
                heading = true,
            )
            AppText(
                text = bill.invoiceDate.text() + " · " + stringResource(Res.string.bills_paid_by, payer),
                style = AppTheme.type.caption,
                color = AppTheme.colors.onSurfaceVariant,
            )
            AppText(
                text = bill.status.label() + " · " + lineCount(bill.lineCount),
                style = AppTheme.type.caption,
                color = AppTheme.colors.onSurfaceVariant,
            )
            AmountText(amount = bill.total)
        }
    }
}

@Composable
private fun lineCount(count: Int): String =
    if (count == 1) stringResource(Res.string.bills_line_one) else stringResource(Res.string.bills_lines_count, count)

/** What it was for, and the total; creating the bill opens it so it can be split. */
@Composable
fun NewBillContent(form: NewBillForm, onEvent: (BillsUiEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppTextField(
            value = form.title,
            onValueChange = { onEvent(BillsUiEvent.TitleChanged(it)) },
            label = stringResource(Res.string.bills_new_name),
            error = form.titleError?.text(),
            modifier = Modifier.fillMaxWidth(),
        )
        AppTextField(
            value = form.amountText,
            onValueChange = { onEvent(BillsUiEvent.AmountChanged(it)) },
            label = stringResource(Res.string.bills_new_amount),
            error = form.amountError?.text(),
            keyboard = AppKeyboard.DECIMAL,
            modifier = Modifier.fillMaxWidth(),
        )
        AppButton(
            text = stringResource(Res.string.bills_new_save),
            onClick = { onEvent(BillsUiEvent.SaveNew) },
            loading = form.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        form.error?.let { AppText(it.text(), color = AppTheme.colors.danger) }
    }
}
