package app.rasoi.core.domain.bills

import app.rasoi.core.model.AppResult
import app.rasoi.core.model.ApprovalState
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillSummary
import app.rasoi.core.model.HouseholdId

interface BillRepository {
    /** Uploads PDFs of one order; the server parses them into one draft bill (design-plan 6.5). */
    suspend fun importInvoices(householdId: HouseholdId, files: List<InvoiceFile>): AppResult<Bill>

    /** The household's bills, newest first; other members' drafts are not included. */
    suspend fun bills(householdId: HouseholdId): AppResult<List<BillSummary>>

    suspend fun createManual(householdId: HouseholdId, draft: ManualBillDraft): AppResult<Bill>

    suspend fun bill(id: BillId): AppResult<Bill>

    suspend fun update(id: BillId, edit: BillEdit): AppResult<Bill>

    suspend fun submit(id: BillId): AppResult<Bill>

    suspend fun decide(id: BillId, state: ApprovalState, comment: String?): AppResult<Bill>
}
