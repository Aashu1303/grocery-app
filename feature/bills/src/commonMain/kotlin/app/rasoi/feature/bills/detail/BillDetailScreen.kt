package app.rasoi.feature.bills.detail

import androidx.compose.foundation.background
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
import app.rasoi.core.designsystem.component.AppButton
import app.rasoi.core.designsystem.component.AppButtonVariant
import app.rasoi.core.designsystem.component.AppCard
import app.rasoi.core.designsystem.component.AppPullToRefresh
import app.rasoi.core.designsystem.component.AppText
import app.rasoi.core.designsystem.component.AppTopBar
import app.rasoi.core.designsystem.component.ApprovalBanner
import app.rasoi.core.designsystem.component.BillLineRow
import app.rasoi.core.designsystem.component.ErrorView
import app.rasoi.core.designsystem.component.LoadingView
import app.rasoi.core.designsystem.component.NoticeBanner
import app.rasoi.core.designsystem.component.NoticeTone
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.model.ApprovalState
import app.rasoi.core.model.BillCharge
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillLine
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.ChargeDistribution
import app.rasoi.core.model.Role
import app.rasoi.core.model.UserId
import app.rasoi.feature.bills.label
import app.rasoi.feature.bills.resources.Res
import app.rasoi.feature.bills.resources.banner_approved_automatic
import app.rasoi.feature.bills.resources.banner_approved_everyone
import app.rasoi.feature.bills.resources.banner_changes_from
import app.rasoi.feature.bills.resources.banner_draft_message
import app.rasoi.feature.bills.resources.banner_draft_title
import app.rasoi.feature.bills.resources.banner_locked_message
import app.rasoi.feature.bills.resources.banner_locked_title
import app.rasoi.feature.bills.resources.banner_mismatch_message
import app.rasoi.feature.bills.resources.banner_mismatch_title
import app.rasoi.feature.bills.resources.banner_pending_progress
import app.rasoi.feature.bills.resources.bills_paid_by
import app.rasoi.feature.bills.resources.charge_equal
import app.rasoi.feature.bills.resources.charge_proportional
import app.rasoi.feature.bills.resources.detail_change_payer
import app.rasoi.feature.bills.resources.detail_charges
import app.rasoi.feature.bills.resources.detail_items
import app.rasoi.feature.bills.resources.detail_load_error
import app.rasoi.feature.bills.resources.detail_non_food
import app.rasoi.feature.bills.resources.detail_not_split
import app.rasoi.feature.bills.resources.detail_title
import app.rasoi.feature.bills.text
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun BillDetailRoute(
    billId: BillId,
    myUserId: UserId,
    myRole: Role,
    onBack: () -> Unit,
    onEditItems: (BillId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BillDetailViewModel = koinViewModel(key = billId.value) { parametersOf(billId, myUserId, myRole) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.onEvent(BillDetailUiEvent.Refresh)
        viewModel.effects.collect { effect ->
            when (effect) {
                is BillDetailEffect.OpenItems -> onEditItems(effect.billId)
            }
        }
    }
    BillDetailScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack, modifier = modifier)
}

/** One bill: status, items with who shares them, what each person owes, and the next step for the viewer. */
@Composable
fun BillDetailScreen(
    state: BillDetailUiState,
    onEvent: (BillDetailUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(AppTheme.colors.surface)) {
        AppTopBar(title = stringResource(Res.string.detail_title), onBack = onBack)
        when (state) {
            BillDetailUiState.Loading -> LoadingView(Modifier.weight(1f))
            is BillDetailUiState.Error -> ErrorView(
                message = stringResource(Res.string.detail_load_error, state.error.text()),
                onRetry = { onEvent(BillDetailUiEvent.Retry) },
                modifier = Modifier.weight(1f),
            )
            is BillDetailUiState.Content -> {
                AppPullToRefresh(
                    refreshing = state.isRefreshing,
                    onRefresh = { onEvent(BillDetailUiEvent.PullToRefresh) },
                    modifier = Modifier.weight(1f),
                ) { DetailContent(state, onEvent, Modifier.fillMaxSize()) }
                BillSheets(state, onEvent)
            }
        }
    }
}

@Composable
private fun DetailContent(state: BillDetailUiState.Content, onEvent: (BillDetailUiEvent) -> Unit, modifier: Modifier) {
    val bill = state.bill
    val names = state.names
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(AppTheme.spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        item { Header(state, onEvent) }
        item { StatusBanner(state) }
        if (state.sheet ==
            null
        ) {
            state.error?.let { error -> item { AppText(error.text(), color = AppTheme.colors.danger) } }
        }
        item { SectionTitle(stringResource(Res.string.detail_items)) }
        items(bill.lines, key = { it.id.value }) { line ->
            LineRow(line, state, names) { onEvent(BillDetailUiEvent.OpenSplit(line.id)) }
        }
        if (bill.charges.isNotEmpty()) {
            item { SectionTitle(stringResource(Res.string.detail_charges)) }
            items(bill.charges, key = { it.id.value }) { charge ->
                ChargeRow(charge, state.canEdit) { onEvent(BillDetailUiEvent.OpenCharge(charge.id)) }
            }
        }
        item { SharesSection(state) }
        if (bill.approvals.isNotEmpty()) item { ApprovalsSection(state) }
        item { Actions(state, onEvent) }
    }
}

@Composable
internal fun SectionTitle(text: String) {
    AppText(text, style = AppTheme.type.label, color = AppTheme.colors.onSurfaceVariant, heading = true)
}

@Composable
private fun Header(state: BillDetailUiState.Content, onEvent: (BillDetailUiEvent) -> Unit) {
    val bill = state.bill
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        ) {
            AppText(bill.title ?: bill.platform.label(), style = AppTheme.type.title, heading = true)
            AppText(
                text = bill.invoiceDate.text() + " · " + bill.status.label(),
                style = AppTheme.type.caption,
                color = AppTheme.colors.onSurfaceVariant,
            )
            AmountText(amount = bill.total, style = AppTheme.type.title)
            AppText(stringResource(Res.string.bills_paid_by, state.names[bill.payerId].orEmpty()))
            if (state.canEdit) {
                AppButton(
                    text = stringResource(Res.string.detail_change_payer),
                    onClick = { onEvent(BillDetailUiEvent.OpenPayer) },
                    variant = AppButtonVariant.TEXT,
                )
            }
        }
    }
}

@Composable
private fun StatusBanner(state: BillDetailUiState.Content) {
    val bill = state.bill
    val mismatch = state.mismatch
    if (mismatch != null && bill.status != BillStatus.LOCKED) {
        NoticeBanner(
            tone = NoticeTone.WARNING,
            title = stringResource(Res.string.banner_mismatch_title),
            message = stringResource(
                Res.string.banner_mismatch_message,
                mismatch.computed.format(),
                mismatch.printed.format(),
            ),
        )
        return
    }
    when (bill.status) {
        BillStatus.DRAFT -> NoticeBanner(
            tone = NoticeTone.INFO,
            title = stringResource(Res.string.banner_draft_title),
            message = stringResource(Res.string.banner_draft_message),
        )
        BillStatus.PENDING -> {
            val approved = bill.approvals.count { it.state == ApprovalState.APPROVED }
            ApprovalBanner(
                ApprovalState.PENDING,
                stringResource(Res.string.banner_pending_progress, approved, bill.approvals.size),
            )
        }
        BillStatus.CHANGES_REQUESTED -> {
            val request = bill.approvals.firstOrNull { it.state == ApprovalState.CHANGES_REQUESTED }
            val who = state.names[request?.userId].orEmpty()
            ApprovalBanner(
                ApprovalState.CHANGES_REQUESTED,
                stringResource(Res.string.banner_changes_from, who, request?.comment.orEmpty()),
            )
        }
        BillStatus.APPROVED -> ApprovalBanner(
            ApprovalState.APPROVED,
            stringResource(
                if (bill.approvals.any { it.automatic }) {
                    Res.string.banner_approved_automatic
                } else {
                    Res.string.banner_approved_everyone
                },
            ),
        )
        BillStatus.LOCKED -> NoticeBanner(
            tone = NoticeTone.INFO,
            title = stringResource(Res.string.banner_locked_title),
            message = stringResource(Res.string.banner_locked_message),
        )
    }
}

@Composable
private fun LineRow(line: BillLine, state: BillDetailUiState.Content, names: Map<UserId, String>, onClick: () -> Unit) {
    val owners = state.bill.allocations.filter { it.lineId == line.id }.map { names[it.userId].orEmpty() }
    BillLineRow(
        name = line.displayName,
        amount = line.amount,
        allocationSummary = if (owners.isEmpty()) {
            stringResource(
                Res.string.detail_not_split,
            )
        } else {
            owners.joinToString()
        },
        tag = if (line.isFood) null else stringResource(Res.string.detail_non_food),
        onClick = if (state.canEdit) onClick else null,
    )
}

@Composable
private fun ChargeRow(charge: BillCharge, canEdit: Boolean, onClick: () -> Unit) {
    BillLineRow(
        name = charge.label,
        amount = charge.amount,
        allocationSummary = stringResource(
            when (charge.distribution) {
                ChargeDistribution.PROPORTIONAL -> Res.string.charge_proportional
                ChargeDistribution.EQUAL -> Res.string.charge_equal
            },
        ),
        onClick = if (canEdit) onClick else null,
    )
}
