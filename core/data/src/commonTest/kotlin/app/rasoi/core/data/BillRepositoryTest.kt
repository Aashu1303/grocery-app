package app.rasoi.core.data

import app.rasoi.core.contract.ApiHeaders
import app.rasoi.core.contract.ContractJson
import app.rasoi.core.contract.bills.UpdateBillRequestDto
import app.rasoi.core.contract.bills.UploadInvoicesRequestDto
import app.rasoi.core.data.repository.DefaultBillRepository
import app.rasoi.core.domain.bills.BillEdit
import app.rasoi.core.domain.bills.InvoiceFile
import app.rasoi.core.domain.bills.LineEdit
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.ImportConfidence
import app.rasoi.core.model.Money
import app.rasoi.core.model.Platform
import app.rasoi.core.model.Quantity
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BillRepositoryTest {
    private val billJson = """
        {"id":"b1","householdId":"h1","platform":"HANDPICKD","invoiceNumber":"OD-1","orderId":"OD-1",
         "invoiceDate":"2026-09-23","payerId":"u1","createdBy":"u1","status":"DRAFT","confidence":"MEDIUM",
         "lines":[{"id":"l1","rawName":"Beans Lobia","displayName":"Beans","units":1,"amountPaise":2800,
                   "measure":{"amount":360000,"unit":"MILLIGRAM"},"hsn":"0708"}],
         "charges":[{"id":"c1","kind":"DELIVERY","label":"Delivery Fee","amountPaise":1900}],
         "totalPaise":4700,"warnings":[{"code":"TOTAL_MISMATCH","computedPaise":4700,"printedPaise":4700}]}
    """.trimIndent()

    private fun body(server: MockServer, index: Int = 0) = (server.requests[index].body as TextContent).text

    @Test
    fun importEncodesTheFilesAndMapsTheDraft() = runTest {
        val server = MockServer { json(billJson, HttpStatusCode.Created) }
        val result = DefaultBillRepository(server.api)
            .importInvoices(HouseholdId("h1"), listOf(InvoiceFile("a.pdf", byteArrayOf(1, 2, 3))))
        val bill = assertIs<AppResult.Success<Bill>>(result).value
        assertEquals(Platform.HANDPICKD, bill.platform)
        assertEquals(LocalDate(2026, 9, 23), bill.invoiceDate)
        assertEquals(BillStatus.DRAFT to ImportConfidence.MEDIUM, bill.status to bill.confidence)
        assertEquals(Quantity.grams(360), bill.lines.single().measure)
        assertEquals(ChargeKind.DELIVERY, bill.charges.single().kind)
        assertEquals(Money(4_700), bill.total)

        val request = server.requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("https://api.rasoi.test/households/h1/invoices", request.url.toString())
        assertEquals("key-1", request.headers[ApiHeaders.IDEMPOTENCY_KEY])
        val sent = ContractJson.decodeFromString(UploadInvoicesRequestDto.serializer(), body(server))
        assertEquals("AQID", sent.documents.single().contentBase64)
    }

    @Test
    fun updatePatchesWithAnIdempotencyKeyAndGetReads() = runTest {
        val server = MockServer { json(billJson) }
        val repository = DefaultBillRepository(server.api)
        val edit = BillEdit(lines = listOf(LineEdit(BillLineId("l1"), "Beans", 2, Money(2_800))), total = Money(4_700))
        assertIs<AppResult.Success<Bill>>(repository.update(BillId("b1"), edit))
        assertIs<AppResult.Success<Bill>>(repository.bill(BillId("b1")))

        val (patch, get) = server.requests
        assertEquals(HttpMethod.Patch to "https://api.rasoi.test/bills/b1", patch.method to patch.url.toString())
        assertEquals("key-1", patch.headers[ApiHeaders.IDEMPOTENCY_KEY])
        val sent = ContractJson.decodeFromString(UpdateBillRequestDto.serializer(), body(server))
        assertEquals(4_700L, sent.totalPaise)
        assertEquals("l1" to 2, sent.lines?.single()?.let { it.id to it.units })
        assertEquals(null, sent.charges)
        assertEquals(HttpMethod.Get, get.method)
    }

    @Test
    fun duplicateUploadsComeBackAsConflicts() = runTest {
        val server = MockServer { json("""{"code":"duplicate_invoice","message":"x"}""", HttpStatusCode.Conflict) }
        val result = DefaultBillRepository(
            server.api,
        ).importInvoices(HouseholdId("h1"), listOf(InvoiceFile("a.pdf", byteArrayOf(1))))
        assertEquals(AppResult.Failure(AppError.Conflict("duplicate_invoice")), result)
    }
}
