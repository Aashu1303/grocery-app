package app.rasoi.feature.bills.list

import app.rasoi.core.model.BillId

sealed interface BillsEffect {
    data class OpenBill(val billId: BillId) : BillsEffect

    data object OpenImport : BillsEffect
}
