package app.rasoi.feature.bills.list

import app.rasoi.core.model.BillId

sealed interface BillsUiEvent {
    data object Retry : BillsUiEvent

    /** The user pulled the screen down: reload, showing the refresh indicator until it is done. */
    data object PullToRefresh : BillsUiEvent

    /** Reloads quietly when the screen comes back, so the list never shows a stale status. */
    data object Refresh : BillsUiEvent

    data class OpenBill(val billId: BillId) : BillsUiEvent

    data object ImportInvoice : BillsUiEvent

    data object OpenNew : BillsUiEvent

    data object DismissNew : BillsUiEvent

    data object SaveNew : BillsUiEvent

    data class TitleChanged(val value: String) : BillsUiEvent

    data class AmountChanged(val value: String) : BillsUiEvent
}
