package app.rasoi.core.data.network

import app.rasoi.core.contract.bills.BillDto
import app.rasoi.core.contract.bills.BillSummaryDto
import app.rasoi.core.contract.bills.CreateBillRequestDto
import app.rasoi.core.contract.bills.DecideApprovalRequestDto
import app.rasoi.core.contract.bills.UpdateBillRequestDto
import app.rasoi.core.contract.bills.UploadInvoicesRequestDto
import app.rasoi.core.model.AppResult
import io.ktor.http.HttpMethod
import kotlinx.serialization.builtins.ListSerializer

// Invoice import, bills, splits and approvals (design-plan 6.6).
suspend fun RasoiApi.importInvoices(householdId: String, request: UploadInvoicesRequestDto): AppResult<BillDto> =
    write("/households/$householdId/invoices", BillDto.serializer()) { jsonBody(request) }

suspend fun RasoiApi.bills(householdId: String): AppResult<List<BillSummaryDto>> = send(
    HttpMethod.Get,
    "/households/$householdId/bills",
    ListSerializer(BillSummaryDto.serializer()),
)

suspend fun RasoiApi.createBill(householdId: String, request: CreateBillRequestDto): AppResult<BillDto> =
    write("/households/$householdId/bills", BillDto.serializer()) { jsonBody(request) }

suspend fun RasoiApi.submitBill(id: String): AppResult<BillDto> =
    write("/bills/$id/submit", BillDto.serializer()) { jsonBody(emptyMap<String, String>()) }

suspend fun RasoiApi.decideBill(id: String, request: DecideApprovalRequestDto): AppResult<BillDto> =
    write("/bills/$id/approvals", BillDto.serializer()) { jsonBody(request) }

suspend fun RasoiApi.bill(id: String): AppResult<BillDto> = send(HttpMethod.Get, "/bills/$id", BillDto.serializer())

suspend fun RasoiApi.updateBill(id: String, request: UpdateBillRequestDto): AppResult<BillDto> =
    write("/bills/$id", BillDto.serializer(), HttpMethod.Patch) { jsonBody(request) }
