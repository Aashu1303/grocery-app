package app.rasoi.server.routes

import app.rasoi.core.contract.bills.BillDto
import app.rasoi.core.contract.bills.BillSummaryDto
import app.rasoi.core.contract.bills.CreateBillRequestDto
import app.rasoi.core.contract.bills.DecideApprovalRequestDto
import app.rasoi.core.contract.bills.UpdateBillRequestDto
import app.rasoi.core.contract.bills.UploadInvoicesRequestDto
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.ApprovalState
import app.rasoi.core.model.BillId
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.flatMap
import app.rasoi.core.model.map
import app.rasoi.server.http.Idempotency
import app.rasoi.server.http.respondError
import app.rasoi.server.http.respondResult
import app.rasoi.server.http.toDto
import app.rasoi.server.http.toJsonResponse
import app.rasoi.server.http.toModel
import app.rasoi.server.http.toSummaryDto
import app.rasoi.server.persistence.parseUuid
import app.rasoi.server.service.BillService
import app.rasoi.server.service.InvoiceImportService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.util.Base64

/** Invoice import and bill endpoints (design-plan 6.6, ADR 0018). */
internal fun Route.billRoutes(imports: InvoiceImportService, bills: BillService, idempotency: Idempotency) {
    post("/households/{id}/invoices") {
        val householdId = call.parameters["id"]?.let(::parseUuid)?.let { HouseholdId(it.toString()) }
            ?: return@post call.respondError(AppError.NotFound("household"))
        idempotency.handle(call, call.userId(), UploadInvoicesRequestDto.serializer(), MAX_UPLOAD_BYTES) { request ->
            decode(request)
                .flatMap { documents -> imports.import(call.userId(), householdId, documents) }
                .map { it.toDto() }
                .toJsonResponse(BillDto.serializer(), HttpStatusCode.Created)
        }
    }

    get("/households/{id}/bills") {
        val householdId = call.householdId() ?: return@get call.respondError(AppError.NotFound("household"))
        call.respondResult(
            bills.list(call.userId(), householdId).map { list -> list.map { it.toSummaryDto() } },
            ListSerializer(BillSummaryDto.serializer()),
        )
    }

    post("/households/{id}/bills") {
        val householdId = call.householdId() ?: return@post call.respondError(AppError.NotFound("household"))
        idempotency.handle(call, call.userId(), CreateBillRequestDto.serializer()) { request ->
            invoiceDate(request.invoiceDate)
                .flatMap { date ->
                    bills.createManual(call.userId(), householdId, request.toModel(call.userId(), date))
                }
                .map { it.toDto() }
                .toJsonResponse(BillDto.serializer(), HttpStatusCode.Created)
        }
    }

    get("/bills/{id}") {
        val billId = call.billId() ?: return@get call.respondError(AppError.NotFound("bill"))
        call.respondResult(bills.get(call.userId(), billId).map { it.toDto() }, BillDto.serializer())
    }

    patch("/bills/{id}") {
        val billId = call.billId() ?: return@patch call.respondError(AppError.NotFound("bill"))
        idempotency.handle(call, call.userId(), UpdateBillRequestDto.serializer()) { request ->
            bills.update(call.userId(), billId, request.toModel())
                .map { it.toDto() }
                .toJsonResponse(BillDto.serializer(), HttpStatusCode.OK)
        }
    }

    post("/bills/{id}/submit") {
        val billId = call.billId() ?: return@post call.respondError(AppError.NotFound("bill"))
        idempotency.handle(call, call.userId(), Unit.serializer()) {
            bills.submit(call.userId(), billId)
                .map { it.toDto() }
                .toJsonResponse(BillDto.serializer(), HttpStatusCode.OK)
        }
    }

    post("/bills/{id}/approvals") {
        val billId = call.billId() ?: return@post call.respondError(AppError.NotFound("bill"))
        idempotency.handle(call, call.userId(), DecideApprovalRequestDto.serializer()) { request ->
            bills.decide(call.userId(), billId, ApprovalState.valueOf(request.state.name), request.comment)
                .map { it.toDto() }
                .toJsonResponse(BillDto.serializer(), HttpStatusCode.OK)
        }
    }
}

/** Five PDFs of about 2 MB each, Base64-encoded. */
private const val MAX_UPLOAD_BYTES = 15L * 1024 * 1024

private fun decode(request: UploadInvoicesRequestDto): AppResult<List<ByteArray>> {
    val decoder = Base64.getDecoder()
    val documents = request.documents.mapIndexed { index, document ->
        try {
            decoder.decode(document.contentBase64)
        } catch (_: IllegalArgumentException) {
            return AppResult.Failure(
                AppError.Validation(listOf(AppError.FieldViolation("documents[$index]", "invalid_base64"))),
            )
        }
    }
    return AppResult.Success(documents)
}

private fun invoiceDate(raw: String): AppResult<LocalDate> = try {
    AppResult.Success(LocalDate.parse(raw))
} catch (_: IllegalArgumentException) {
    AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("invoiceDate", "invalid"))))
}

private fun ApplicationCall.householdId(): HouseholdId? =
    parameters["id"]?.let(::parseUuid)?.let { HouseholdId(it.toString()) }

private fun ApplicationCall.billId(): BillId? = parameters["id"]?.let(::parseUuid)?.let { BillId(it.toString()) }
