package app.rasoi.feature.invoiceimport.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.rasoi.core.designsystem.component.AmountText
import app.rasoi.core.designsystem.component.AppBottomSheet
import app.rasoi.core.designsystem.component.AppButton
import app.rasoi.core.designsystem.component.AppButtonVariant
import app.rasoi.core.designsystem.component.AppText
import app.rasoi.core.designsystem.component.AppTopBar
import app.rasoi.core.designsystem.component.BillLineRow
import app.rasoi.core.designsystem.component.ErrorView
import app.rasoi.core.designsystem.component.LoadingView
import app.rasoi.core.designsystem.component.NoticeBanner
import app.rasoi.core.designsystem.component.NoticeTone
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillLine
import app.rasoi.core.model.BillWarning
import app.rasoi.feature.invoiceimport.label
import app.rasoi.feature.invoiceimport.measureText
import app.rasoi.feature.invoiceimport.resources.Res
import app.rasoi.feature.invoiceimport.resources.months_short
import app.rasoi.feature.invoiceimport.resources.review_charges
import app.rasoi.feature.invoiceimport.resources.review_date_format
import app.rasoi.feature.invoiceimport.resources.review_done
import app.rasoi.feature.invoiceimport.resources.review_edit_title
import app.rasoi.feature.invoiceimport.resources.review_fallback_message
import app.rasoi.feature.invoiceimport.resources.review_fallback_title
import app.rasoi.feature.invoiceimport.resources.review_invoice
import app.rasoi.feature.invoiceimport.resources.review_lines
import app.rasoi.feature.invoiceimport.resources.review_load_error
import app.rasoi.feature.invoiceimport.resources.review_mismatch_message
import app.rasoi.feature.invoiceimport.resources.review_mismatch_title
import app.rasoi.feature.invoiceimport.resources.review_quantity
import app.rasoi.feature.invoiceimport.resources.review_quantity_measure
import app.rasoi.feature.invoiceimport.resources.review_title
import app.rasoi.feature.invoiceimport.resources.review_total
import app.rasoi.feature.invoiceimport.resources.review_use_calculated
import app.rasoi.feature.invoiceimport.text
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun BillReviewRoute(
    billId: BillId,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BillReviewViewModel = koinViewModel(key = billId.value) { parametersOf(billId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BillReviewScreen(state = state, onEvent = viewModel::onEvent, onDone = onDone, modifier = modifier)
}

/** What the server read from the invoice, with warnings to resolve and a sheet to correct an item. */
@Composable
fun BillReviewScreen(
    state: BillReviewUiState,
    onEvent: (BillReviewUiEvent) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(AppTheme.colors.surface)) {
        AppTopBar(title = stringResource(Res.string.review_title), onBack = onDone)
        when (state) {
            BillReviewUiState.Loading -> LoadingView()
            is BillReviewUiState.Error -> ErrorView(
                message = stringResource(Res.string.review_load_error, state.error.text()),
                onRetry = { onEvent(BillReviewUiEvent.Retry) },
            )
            is BillReviewUiState.Content -> BillContent(state, onEvent, onDone)
        }
    }
    (state as? BillReviewUiState.Content)?.edit?.let { edit ->
        AppBottomSheet(
            title = stringResource(Res.string.review_edit_title),
            onDismiss = { onEvent(BillReviewUiEvent.DismissEdit) },
        ) { LineEditContent(edit, onEvent) }
    }
}

@Composable
private fun BillContent(state: BillReviewUiState.Content, onEvent: (BillReviewUiEvent) -> Unit, onDone: () -> Unit) {
    val bill = state.bill
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppTheme.spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        item { BillHeader(bill) }
        items(state.warnings, key = { it::class.simpleName.orEmpty() }) { warning ->
            WarningNotice(warning, state.isSaving, onEvent)
        }
        item { SectionTitle(stringResource(Res.string.review_lines)) }
        items(bill.lines, key = { "line-${it.id.value}" }) { line ->
            LineRow(line, onClick = { onEvent(BillReviewUiEvent.EditLine(line.id)) })
        }
        if (bill.charges.isNotEmpty()) {
            item { SectionTitle(stringResource(Res.string.review_charges)) }
            items(bill.charges, key = { "charge-${it.id.value}" }) { charge ->
                BillLineRow(name = charge.label, amount = charge.amount, allocationSummary = charge.kind.label())
            }
        }
        item { TotalRow(bill) }
        state.error?.let { error -> item { AppText(error.text(), color = AppTheme.colors.danger) } }
        item {
            AppButton(
                text = stringResource(Res.string.review_done),
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BillHeader(bill: Bill) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs)) {
        AppText(bill.platform.label(), style = AppTheme.type.title, heading = true)
        AppText(
            stringResource(Res.string.review_invoice, bill.invoiceNumber),
            style = AppTheme.type.caption,
            color = AppTheme.colors.onSurfaceVariant,
        )
        AppText(bill.invoiceDate.shortText(), style = AppTheme.type.caption, color = AppTheme.colors.onSurfaceVariant)
    }
}

@Composable
private fun WarningNotice(warning: BillWarning, isSaving: Boolean, onEvent: (BillReviewUiEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        when (warning) {
            is BillWarning.TotalMismatch -> {
                NoticeBanner(
                    tone = NoticeTone.WARNING,
                    title = stringResource(Res.string.review_mismatch_title),
                    message = stringResource(
                        Res.string.review_mismatch_message,
                        warning.computed.format(),
                        warning.printed.format(),
                    ),
                )
                AppButton(
                    text = stringResource(Res.string.review_use_calculated),
                    onClick = { onEvent(BillReviewUiEvent.UseCalculatedTotal) },
                    variant = AppButtonVariant.SECONDARY,
                    loading = isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            BillWarning.ParsedByFallback -> NoticeBanner(
                tone = NoticeTone.INFO,
                title = stringResource(Res.string.review_fallback_title),
                message = stringResource(Res.string.review_fallback_message),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    AppText(text, style = AppTheme.type.title, heading = true)
}

@Composable
private fun LineRow(line: BillLine, onClick: () -> Unit) {
    val summary = line.measure?.let { stringResource(Res.string.review_quantity_measure, line.units, it.measureText()) }
        ?: stringResource(Res.string.review_quantity, line.units)
    BillLineRow(name = line.displayName, amount = line.amount, allocationSummary = summary, onClick = onClick)
}

@Composable
private fun TotalRow(bill: Bill) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppTheme.spacing.minTouchTarget)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(stringResource(Res.string.review_total), style = AppTheme.type.title, modifier = Modifier.weight(1f))
        AmountText(bill.total)
    }
}

@Composable
private fun LocalDate.shortText(): String {
    val months = stringArrayResource(Res.array.months_short)
    return stringResource(Res.string.review_date_format, day, months[month.ordinal])
}
