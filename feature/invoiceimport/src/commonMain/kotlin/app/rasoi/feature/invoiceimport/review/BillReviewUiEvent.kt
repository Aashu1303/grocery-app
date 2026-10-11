package app.rasoi.feature.invoiceimport.review

import app.rasoi.core.model.BillLineId

sealed interface BillReviewUiEvent {
    data object Retry : BillReviewUiEvent

    data class EditLine(val lineId: BillLineId) : BillReviewUiEvent

    data class NameChanged(val value: String) : BillReviewUiEvent

    data class UnitsStepped(val delta: Int) : BillReviewUiEvent

    data class AmountChanged(val value: String) : BillReviewUiEvent

    /** Whether the item goes to the pantry. */
    data class FoodChanged(val isFood: Boolean) : BillReviewUiEvent

    data object SaveLine : BillReviewUiEvent

    data object DismissEdit : BillReviewUiEvent

    /** Accepts lines plus charges as the invoice total, for a mismatch the user has checked. */
    data object UseCalculatedTotal : BillReviewUiEvent
}
