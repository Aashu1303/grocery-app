package app.rasoi.feature.bills.detail

import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.ChargeDistribution
import app.rasoi.core.model.ChargeId
import app.rasoi.core.model.SplitMode
import app.rasoi.core.model.UserId

sealed interface BillDetailUiEvent {
    data object Retry : BillDetailUiEvent

    /** The user pulled the screen down: reload, showing the refresh indicator until it is done. */
    data object PullToRefresh : BillDetailUiEvent

    /** Reloads quietly when the screen comes back, for example from the item editor. */
    data object Refresh : BillDetailUiEvent

    data object DismissSheet : BillDetailUiEvent

    data class OpenSplit(val lineId: BillLineId) : BillDetailUiEvent

    data object OpenPayer : BillDetailUiEvent

    data class OpenCharge(val chargeId: ChargeId) : BillDetailUiEvent

    data object EditItems : BillDetailUiEvent

    data object Submit : BillDetailUiEvent

    data object Approve : BillDetailUiEvent

    data object OpenRequestChanges : BillDetailUiEvent

    /** Edits to the open split sheet. */
    sealed interface Split : BillDetailUiEvent {
        data class ModeSelected(val mode: SplitMode) : Split

        data class MemberToggled(val userId: UserId) : Split

        data class UnitsStepped(val userId: UserId, val delta: Int) : Split

        data object Save : Split
    }

    /** Choices made in the payer, charge and change-request sheets. */
    sealed interface Change : BillDetailUiEvent {
        data class PayerSelected(val userId: UserId) : Change

        data class DistributionSelected(val distribution: ChargeDistribution) : Change

        data class CommentChanged(val value: String) : Change

        data object SendChangeRequest : Change
    }
}
