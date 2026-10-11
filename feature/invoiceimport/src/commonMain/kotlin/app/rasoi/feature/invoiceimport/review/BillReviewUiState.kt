package app.rasoi.feature.invoiceimport.review

import app.rasoi.core.domain.bills.BillRules
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.BillWarning
import app.rasoi.feature.invoiceimport.BillError

sealed interface BillReviewUiState {
    data object Loading : BillReviewUiState

    data class Error(val error: BillError) : BillReviewUiState

    /** A draft bill. [warnings] are derived from its current lines, so fixing an amount clears them. */
    data class Content(
        val bill: Bill,
        val edit: LineEditState? = null,
        val isSaving: Boolean = false,
        val error: BillError? = null,
    ) : BillReviewUiState {
        val warnings: List<BillWarning> get() = BillRules.warnings(bill)
    }
}

enum class LineFieldError { REQUIRED, TOO_LONG, INVALID }

/** The line being edited in the sheet; [amountText] is what the user typed, parsed only on save. */
data class LineEditState(
    val lineId: BillLineId,
    val name: String,
    val units: Int,
    val amountText: String,
    val isFood: Boolean,
    val nameError: LineFieldError? = null,
    val amountError: LineFieldError? = null,
    val error: BillError? = null,
    val isSaving: Boolean = false,
)
