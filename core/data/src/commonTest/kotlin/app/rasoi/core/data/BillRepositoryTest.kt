package app.rasoi.core.data

import app.rasoi.core.contract.ApiHeaders
import app.rasoi.core.contract.ContractJson
import app.rasoi.core.contract.bills.ApprovalStateDto
import app.rasoi.core.contract.bills.ChargeDistributionDto
import app.rasoi.core.contract.bills.CreateBillRequestDto
import app.rasoi.core.contract.bills.DecideApprovalRequestDto
import app.rasoi.core.contract.bills.SplitModeDto
import app.rasoi.core.contract.bills.UpdateBillRequestDto
import app.rasoi.core.contract.bills.UploadInvoicesRequestDto
import app.rasoi.core.data.repository.DefaultBillRepository
import app.rasoi.core.domain.bills.BillEdit
import app.rasoi.core.domain.bills.ChargeEdit
import app.rasoi.core.domain.bills.InvoiceFile
import app.rasoi.core.domain.bills.LineEdit
import app.rasoi.core.domain.bills.LineSplitEdit
import app.rasoi.core.domain.bills.ManualBillDraft
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.ApprovalState
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.ChargeDistribution
import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.ImportConfidence
import app.rasoi.core.model.LineSplit
import app.rasoi.core.model.Money
import app.rasoi.core.model.Platform
import app.rasoi.core.model.Quantity
import app.rasoi.core.model.SplitEntry
import app.rasoi.core.model.SplitMode
import app.rasoi.core.model.UserId
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

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

    private val splitBillJson = """
        {"id":"b2","householdId":"h1","platform":"MANUAL","invoiceNumber":"manual-b2","title":"Gas cylinder",
         "invoiceDate":"2026-10-05","payerId":"u1","createdBy":"u1","status":"PENDING","confidence":"HIGH",
         "lines":[{"id":"l1","rawName":"Cylinder","displayName":"Cylinder","units":1,"amountPaise":95000}],
         "charges":[{"id":"c1","kind":"DELIVERY","label":"Delivery","amountPaise":3000,"distribution":"EQUAL"}],
         "totalPaise":98000,"warnings":[],
         "allocations":[{"lineId":"l1","userId":"u1","mode":"EQUAL","units":1,"amountPaise":47500},
                        {"lineId":"l1","userId":"u2","mode":"EQUAL","units":1,"amountPaise":47500}],
         "approvals":[{"userId":"u2","state":"CHANGES_REQUESTED","comment":"Too much","decidedAt":"2026-10-06T08:00:00Z"}],
         "shares":[],"submittedAt":"2026-10-05T10:00:00Z"}
    """.trimIndent()

    @Test
    fun aSplitBillKeepsItsAllocationsApprovalsAndChargeDistribution() = runTest {
        val server = MockServer { json(splitBillJson) }
        val bill = (DefaultBillRepository(server.api).bill(BillId("b2")) as AppResult.Success).value
        assertEquals("Gas cylinder", bill.title)
        assertEquals(ChargeDistribution.EQUAL, bill.charges.single().distribution)
        assertEquals(listOf(Money(47_500), Money(47_500)), bill.allocations.map { it.amount })
        assertEquals(SplitMode.EQUAL, bill.allocations.first().mode)
        val approval = bill.approvals.single()
        assertEquals(ApprovalState.CHANGES_REQUESTED to "Too much", approval.state to approval.comment)
        assertEquals(Instant.parse("2026-10-06T08:00:00Z"), approval.decidedAt)
        assertEquals(Instant.parse("2026-10-05T10:00:00Z"), bill.submittedAt)
    }

    @Test
    fun editsCarryThePayerSplitsAndChargeDistribution() = runTest {
        val server = MockServer { json(splitBillJson) }
        val edit = BillEdit(
            charges = listOf(ChargeEdit(null, ChargeKind.DELIVERY, "Delivery", Money(3_000), ChargeDistribution.EQUAL)),
            payerId = UserId("u2"),
            splits = listOf(
                LineSplitEdit(
                    BillLineId("l1"),
                    LineSplit(SplitMode.QUANTITY, listOf(SplitEntry(UserId("u1"), 2), SplitEntry(UserId("u2"), 1))),
                ),
            ),
        )
        DefaultBillRepository(server.api).update(BillId("b2"), edit)
        val sent = ContractJson.decodeFromString(UpdateBillRequestDto.serializer(), body(server))
        assertEquals("u2", sent.payerId)
        assertEquals(ChargeDistributionDto.EQUAL, sent.charges?.single()?.distribution)
        val split = sent.splits?.single()
        assertEquals("l1" to SplitModeDto.QUANTITY, split?.let { it.lineId to it.mode })
        assertEquals(listOf("u1" to 2, "u2" to 1), split?.entries?.map { it.userId to it.units })
    }

    @Test
    fun listingMapsSummariesAndGetsTheHouseholdsBills() = runTest {
        val server = MockServer {
            json(
                """[{"id":"b2","platform":"MANUAL","title":"Gas cylinder","invoiceDate":"2026-10-05","payerId":"u1",
                    "createdBy":"u1","status":"APPROVED","totalPaise":98000,"lineCount":1}]""",
            )
        }
        val list = (DefaultBillRepository(server.api).bills(HouseholdId("h1")) as AppResult.Success).value
        assertEquals(BillStatus.APPROVED, list.single().status)
        assertEquals("Gas cylinder", list.single().title)
        assertEquals(Money(98_000), list.single().total)
        assertEquals("https://api.rasoi.test/households/h1/bills", server.requests.single().url.toString())
    }

    @Test
    fun aManualBillIsPostedWithoutADateWhenTheDraftHasNone() = runTest {
        val server = MockServer { json(splitBillJson, HttpStatusCode.Created) }
        val draft = ManualBillDraft(
            "Gas cylinder",
            null,
            UserId("u1"),
            listOf(LineEdit(null, "Cylinder", 1, Money(95_000))),
            emptyList(),
        )
        DefaultBillRepository(server.api).createManual(HouseholdId("h1"), draft)
        val request = server.requests.single()
        assertEquals(
            HttpMethod.Post to "https://api.rasoi.test/households/h1/bills",
            request.method to request.url.toString(),
        )
        val sent = ContractJson.decodeFromString(CreateBillRequestDto.serializer(), body(server))
        assertEquals(null, sent.invoiceDate)
        assertEquals("u1", sent.payerId)
        assertEquals(95_000L, sent.lines.single().amountPaise)
    }

    @Test
    fun submitAndDecidePostWithIdempotencyKeys() = runTest {
        val server = MockServer { json(splitBillJson) }
        val repository = DefaultBillRepository(server.api)
        repository.submit(BillId("b2"))
        repository.decide(BillId("b2"), ApprovalState.CHANGES_REQUESTED, "Too much")
        val (submit, decide) = server.requests
        assertEquals("https://api.rasoi.test/bills/b2/submit", submit.url.toString())
        assertEquals("https://api.rasoi.test/bills/b2/approvals", decide.url.toString())
        assertEquals("key-1", submit.headers[ApiHeaders.IDEMPOTENCY_KEY])
        val sent = ContractJson.decodeFromString(DecideApprovalRequestDto.serializer(), body(server, 1))
        assertEquals(ApprovalStateDto.CHANGES_REQUESTED to "Too much", sent.state to sent.comment)
    }

    @Test
    fun aConcurrentChangeComesBackAsAConflict() = runTest {
        val server = MockServer { json("""{"code":"bill_changed","message":"x"}""", HttpStatusCode.Conflict) }
        val result = DefaultBillRepository(server.api).decide(BillId("b2"), ApprovalState.APPROVED, null)
        assertEquals(AppResult.Failure(AppError.Conflict("bill_changed")), result)
    }
}
