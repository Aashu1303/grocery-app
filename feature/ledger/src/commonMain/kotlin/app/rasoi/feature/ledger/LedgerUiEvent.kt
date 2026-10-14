package app.rasoi.feature.ledger

import app.rasoi.core.model.PeriodMonth
import app.rasoi.core.model.SettlementId

sealed interface LedgerUiEvent {
    data object Retry : LedgerUiEvent

    /** The user pulled the screen down: reload, showing the refresh indicator until it is done. */
    data object PullToRefresh : LedgerUiEvent

    /** Reloads quietly when the screen comes back. */
    data object Refresh : LedgerUiEvent

    data class SelectMonth(val month: PeriodMonth) : LedgerUiEvent

    data class PayWithUpi(val settlementId: SettlementId) : LedgerUiEvent

    data class MarkPaid(val settlementId: SettlementId) : LedgerUiEvent

    data object AskClose : LedgerUiEvent

    data object ConfirmClose : LedgerUiEvent

    data object DismissSheet : LedgerUiEvent

    /** The viewer's own UPI address and the UPI app hand-off. */
    sealed interface Upi : LedgerUiEvent {
        data object Edit : Upi

        data class Changed(val value: String) : Upi

        data object Save : Upi

        /** No app on the device could open the payment link. */
        data object Unavailable : Upi
    }
}
