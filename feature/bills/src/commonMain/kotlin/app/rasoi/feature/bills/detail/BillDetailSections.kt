package app.rasoi.feature.bills.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.rasoi.core.designsystem.component.AmountText
import app.rasoi.core.designsystem.component.AppButton
import app.rasoi.core.designsystem.component.AppButtonVariant
import app.rasoi.core.designsystem.component.AppText
import app.rasoi.core.designsystem.component.AvatarSize
import app.rasoi.core.designsystem.component.MemberAvatar
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.model.Approval
import app.rasoi.core.model.ApprovalState
import app.rasoi.feature.bills.resources.Res
import app.rasoi.feature.bills.resources.approval_approved
import app.rasoi.feature.bills.resources.approval_automatic
import app.rasoi.feature.bills.resources.approval_changes
import app.rasoi.feature.bills.resources.approval_pending
import app.rasoi.feature.bills.resources.detail_approvals
import app.rasoi.feature.bills.resources.detail_approve
import app.rasoi.feature.bills.resources.detail_edit_items
import app.rasoi.feature.bills.resources.detail_request_changes
import app.rasoi.feature.bills.resources.detail_shares
import app.rasoi.feature.bills.resources.detail_shares_hint
import app.rasoi.feature.bills.resources.detail_submit
import org.jetbrains.compose.resources.stringResource

/** What each person owes: their items plus their part of the fees and discounts. */
@Composable
internal fun SharesSection(state: BillDetailUiState.Content) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
        SectionTitle(stringResource(Res.string.detail_shares))
        if (state.shares.isEmpty()) {
            AppText(
                stringResource(Res.string.detail_shares_hint),
                style = AppTheme.type.caption,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
        state.shares.forEach { share ->
            val name = state.names[share.userId].orEmpty()
            PersonRow(name) { AmountText(amount = share.total) }
        }
    }
}

/** Where each required approver stands. */
@Composable
internal fun ApprovalsSection(state: BillDetailUiState.Content) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
        SectionTitle(stringResource(Res.string.detail_approvals))
        state.bill.approvals.forEach { approval ->
            PersonRow(state.names[approval.userId].orEmpty()) {
                AppText(approval.text(), style = AppTheme.type.caption, color = AppTheme.colors.onSurfaceVariant)
            }
        }
    }
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
private fun Approval.text(): String = stringResource(
    when {
        state == ApprovalState.APPROVED && automatic -> Res.string.approval_automatic
        state == ApprovalState.APPROVED -> Res.string.approval_approved
        state == ApprovalState.CHANGES_REQUESTED -> Res.string.approval_changes
        else -> Res.string.approval_pending
    },
)

/** The next steps open to the viewer: edit the items, send for approval, approve or ask for changes. */
@Composable
internal fun Actions(state: BillDetailUiState.Content, onEvent: (BillDetailUiEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm), modifier = Modifier.fillMaxWidth()) {
        if (state.canSubmit) {
            AppButton(
                text = stringResource(Res.string.detail_submit),
                onClick = { onEvent(BillDetailUiEvent.Submit) },
                loading = state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.canApprove) {
            AppButton(
                text = stringResource(Res.string.detail_approve),
                onClick = { onEvent(BillDetailUiEvent.Approve) },
                loading = state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.canRequestChanges) {
            AppButton(
                text = stringResource(Res.string.detail_request_changes),
                onClick = { onEvent(BillDetailUiEvent.OpenRequestChanges) },
                variant = AppButtonVariant.SECONDARY,
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.canEditItems) {
            AppButton(
                text = stringResource(Res.string.detail_edit_items),
                onClick = { onEvent(BillDetailUiEvent.EditItems) },
                variant = AppButtonVariant.SECONDARY,
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
