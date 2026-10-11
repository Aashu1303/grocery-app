package app.rasoi.core.data.repository

import app.rasoi.core.contract.bills.ApprovalStateDto
import app.rasoi.core.contract.bills.DecideApprovalRequestDto
import app.rasoi.core.contract.bills.InvoiceDocumentDto
import app.rasoi.core.contract.bills.UploadInvoicesRequestDto
import app.rasoi.core.data.network.RasoiApi
import app.rasoi.core.data.network.bill
import app.rasoi.core.data.network.bills
import app.rasoi.core.data.network.createBill
import app.rasoi.core.data.network.decideBill
import app.rasoi.core.data.network.importInvoices
import app.rasoi.core.data.network.submitBill
import app.rasoi.core.data.network.updateBill
import app.rasoi.core.domain.bills.BillEdit
import app.rasoi.core.domain.bills.BillRepository
import app.rasoi.core.domain.bills.InvoiceFile
import app.rasoi.core.domain.bills.ManualBillDraft
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.ApprovalState
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillSummary
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.map
import kotlin.io.encoding.Base64

class DefaultBillRepository(private val api: RasoiApi) : BillRepository {
    override suspend fun importInvoices(householdId: HouseholdId, files: List<InvoiceFile>): AppResult<Bill> {
        val documents = files.map { InvoiceDocumentDto(it.name, Base64.encode(it.bytes)) }
        return api.importInvoices(householdId.value, UploadInvoicesRequestDto(documents)).map { it.toModel() }
    }

    override suspend fun bills(householdId: HouseholdId): AppResult<List<BillSummary>> =
        api.bills(householdId.value).map { list -> list.map { it.toModel() } }

    override suspend fun createManual(householdId: HouseholdId, draft: ManualBillDraft): AppResult<Bill> =
        api.createBill(householdId.value, draft.toDto()).map { it.toModel() }

    override suspend fun bill(id: BillId): AppResult<Bill> = api.bill(id.value).map { it.toModel() }

    override suspend fun update(id: BillId, edit: BillEdit): AppResult<Bill> =
        api.updateBill(id.value, edit.toDto()).map { it.toModel() }

    override suspend fun submit(id: BillId): AppResult<Bill> = api.submitBill(id.value).map { it.toModel() }

    override suspend fun decide(id: BillId, state: ApprovalState, comment: String?): AppResult<Bill> =
        api.decideBill(id.value, DecideApprovalRequestDto(ApprovalStateDto.valueOf(state.name), comment))
            .map { it.toModel() }
}
