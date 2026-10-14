package app.rasoi.feature.bills.list

import app.rasoi.core.model.BillSummary
import app.rasoi.core.model.UserId
import app.rasoi.feature.bills.BillsError
import app.rasoi.feature.bills.FieldError

sealed interface BillsUiState {
    data object Loading : BillsUiState

    data class Error(val error: BillsError) : BillsUiState

    /** [names] maps members to their display names; [form] is the open "add a bill" sheet. */
    data class Content(
        val bills: List<BillSummary>,
        val names: Map<UserId, String>,
        val canEdit: Boolean,
        val form: NewBillForm? = null,
        /** The user pulled to refresh; the content stays while it reloads. */
        val isRefreshing: Boolean = false,
    ) : BillsUiState
}

/** A bill typed in by hand: what it was for and the total; the date is today and the payer is the creator. */
data class NewBillForm(
    val title: String = "",
    val amountText: String = "",
    val titleError: FieldError? = null,
    val amountError: FieldError? = null,
    val error: BillsError? = null,
    val isSaving: Boolean = false,
)
