package app.rasoi.feature.bills.detail

import app.rasoi.core.model.BillId

sealed interface BillDetailEffect {
    /** Open the line-by-line editor of a draft. */
    data class OpenItems(val billId: BillId) : BillDetailEffect
}
