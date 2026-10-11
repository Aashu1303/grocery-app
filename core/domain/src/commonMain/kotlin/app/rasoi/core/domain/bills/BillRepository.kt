package app.rasoi.core.domain.bills

import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillId
import app.rasoi.core.model.HouseholdId

interface BillRepository {
    /** Uploads PDFs of one order; the server parses them into one draft bill (design-plan 6.5). */
    suspend fun importInvoices(householdId: HouseholdId, files: List<InvoiceFile>): AppResult<Bill>

    suspend fun bill(id: BillId): AppResult<Bill>

    suspend fun update(id: BillId, edit: BillEdit): AppResult<Bill>
}
