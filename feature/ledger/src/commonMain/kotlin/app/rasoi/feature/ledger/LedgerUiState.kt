package app.rasoi.feature.ledger

import app.rasoi.core.domain.household.Permission
import app.rasoi.core.domain.household.RolePolicy
import app.rasoi.core.model.Ledger
import app.rasoi.core.model.LedgerTransfer
import app.rasoi.core.model.PeriodStatus
import app.rasoi.core.model.Role
import app.rasoi.core.model.SettlementStatus
import app.rasoi.core.model.UserId

sealed interface LedgerUiState {
    data object Loading : LedgerUiState

    data class Error(val error: LedgerError) : LedgerUiState

    /** [myUpi] is the viewer's own UPI address; [error] is shown above the balances, or in the open sheet. */
    data class Content(
        val ledger: Ledger,
        val names: Map<UserId, String>,
        val myUserId: UserId,
        val myRole: Role,
        val myUpi: String? = null,
        val sheet: LedgerSheet? = null,
        val isBusy: Boolean = false,
        val error: LedgerError? = null,
    ) : LedgerUiState {
        val canClose: Boolean
            get() = ledger.status == PeriodStatus.OPEN && RolePolicy.allows(myRole, Permission.CLOSE_PERIOD)

        /** Either side of a stored transfer, or an owner, may mark it paid. */
        fun canMarkPaid(item: LedgerTransfer): Boolean = item.settlementId != null &&
            item.status == SettlementStatus.PENDING &&
            (myUserId == item.transfer.from || myUserId == item.transfer.to || myRole == Role.OWNER)

        /** Only the payer pays, only once the transfer is stored, and only if the receiver gave a UPI address. */
        fun canPayByUpi(item: LedgerTransfer): Boolean = item.settlementId != null &&
            item.status == SettlementStatus.PENDING &&
            item.transfer.from == myUserId &&
            item.payeeUpiVpa != null
    }
}

sealed interface LedgerSheet {
    data object ConfirmClose : LedgerSheet

    data class Upi(val text: String, val invalid: Boolean = false) : LedgerSheet
}
