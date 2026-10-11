package app.rasoi.server

import app.rasoi.core.contract.ErrorDto
import app.rasoi.core.contract.auth.SessionDto
import app.rasoi.core.contract.bills.BillChargeEditDto
import app.rasoi.core.contract.bills.BillDto
import app.rasoi.core.contract.bills.BillLineEditDto
import app.rasoi.core.contract.bills.BillStatusDto
import app.rasoi.core.contract.bills.BillWarningCodeDto
import app.rasoi.core.contract.bills.BillWarningDto
import app.rasoi.core.contract.bills.ChargeKindDto
import app.rasoi.core.contract.bills.ImportConfidenceDto
import app.rasoi.core.contract.bills.InvoiceDocumentDto
import app.rasoi.core.contract.bills.PlatformDto
import app.rasoi.core.contract.bills.UpdateBillRequestDto
import app.rasoi.core.contract.bills.UploadInvoicesRequestDto
import app.rasoi.core.contract.household.CreateHouseholdRequestDto
import app.rasoi.core.contract.household.CreateInviteRequestDto
import app.rasoi.core.contract.household.HouseholdDto
import app.rasoi.core.contract.household.InviteDto
import app.rasoi.core.contract.household.RoleDto
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.server.parsing.InvoiceFallback
import app.rasoi.server.support.Invoices
import app.rasoi.server.support.TestScope
import app.rasoi.server.support.authed
import app.rasoi.server.support.rasoiTest
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BillRoutesTest {
    private suspend fun TestScope.household(owner: SessionDto): HouseholdDto = client.post("/households") {
        authed(owner)
        setBody(CreateHouseholdRequestDto("Flat 4B", "Asia/Kolkata"))
    }.body()

    private suspend fun TestScope.join(owner: SessionDto, householdId: String, who: SessionDto, role: RoleDto) {
        val invite: InviteDto = client.post("/households/$householdId/invites") {
            authed(owner)
            setBody(CreateInviteRequestDto(role))
        }.body()
        client.post("/invites/${invite.code}/accept") {
            authed(who)
            setBody("{}")
        }
    }

    private suspend fun TestScope.upload(
        session: SessionDto,
        householdId: String,
        body: UploadInvoicesRequestDto,
        key: String? = java.util.UUID.randomUUID().toString(),
    ): HttpResponse = client.post("/households/$householdId/invoices") {
        authed(session, key)
        setBody(body)
    }

    private suspend fun TestScope.edit(session: SessionDto, billId: String, body: UpdateBillRequestDto): HttpResponse =
        client.patch("/bills/$billId") {
            authed(session)
            setBody(body)
        }

    private suspend fun HttpResponse.error(): ErrorDto = body()

    @Test
    fun anUploadedInvoiceBecomesAReconciledDraftForItsUploader() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val home = household(owner)
        val response = upload(owner, home.id, Invoices.upload(Invoices.pdf("zepto")))
        assertEquals(HttpStatusCode.Created, response.status)
        val bill: BillDto = response.body()
        assertEquals(PlatformDto.ZEPTO, bill.platform)
        assertEquals(BillStatusDto.DRAFT, bill.status)
        assertEquals(ImportConfidenceDto.HIGH, bill.confidence)
        assertEquals("2026-10-10", bill.invoiceDate)
        assertEquals(11, bill.lines.size)
        assertEquals(86_700, bill.totalPaise)
        assertEquals(emptyList(), bill.warnings)
        assertEquals(owner.user.id, bill.payerId)
        assertEquals(1, storage.files.size)

        val fetched: BillDto = client.get("/bills/${bill.id}") { authed(owner, null) }.body()
        assertEquals(bill, fetched)
    }

    @Test
    fun severalPdfsOfOneOrderBecomeOneBill() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val home = household(owner)
        val second = Invoices.pdf("blinkit", "INV0000000000002" to "INV0000000000009")
        val bill: BillDto = upload(owner, home.id, Invoices.upload(Invoices.pdf("blinkit"), second))
            .also { assertEquals(HttpStatusCode.Created, it.status) }
            .body()
        assertEquals("INV0000000000002", bill.invoiceNumber)
        assertEquals(2, bill.lines.size)
        assertEquals(25_800, bill.totalPaise)
        assertEquals(emptyList(), bill.warnings)

        val mixed = upload(owner, home.id, Invoices.upload(Invoices.pdf("zepto"), Invoices.pdf("instamart")))
        assertEquals(HttpStatusCode.BadRequest, mixed.status)
        assertEquals("different_orders", mixed.error().fields.single().code)
    }

    @Test
    fun duplicatesAreRejectedByFileAndByInvoiceNumber() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val home = household(owner)
        upload(owner, home.id, Invoices.upload(Invoices.pdf("handpickd")))
        val sameFile = upload(owner, home.id, Invoices.upload(Invoices.pdf("handpickd")))
        assertEquals(HttpStatusCode.Conflict, sameFile.status)
        assertEquals("duplicate_invoice", sameFile.error().code)
        val sameNumber = upload(owner, home.id, Invoices.upload(Invoices.pdf("handpickd", "Beans" to "Peas")))
        assertEquals("duplicate_invoice", sameNumber.error().code)
        assertEquals(1, storage.files.size)
    }

    @Test
    fun unreadableDocumentsAreReportedByPosition() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val home = household(owner)
        val notPdf = upload(owner, home.id, Invoices.upload(Invoices.pdf("zepto"), "hello".toByteArray()))
        assertEquals(listOf("documents[1]" to "not_a_pdf"), notPdf.error().fields.map { it.field to it.code })
        val badBase64 = upload(owner, home.id, UploadInvoicesRequestDto(listOf(InvoiceDocumentDto("a.pdf", "%%%"))))
        assertEquals("invalid_base64", badBase64.error().fields.single().code)
        val unknown =
            upload(owner, home.id, Invoices.upload(Invoices.pdf("zepto", "ZEPTO" to "ACME", "Geddit" to "Acme")))
        assertEquals("unsupported_format", unknown.error().fields.single().code)
        val none = upload(owner, home.id, UploadInvoicesRequestDto(emptyList()))
        assertEquals("count", none.error().fields.single().code)
        assertTrue(storage.files.isEmpty())
    }

    @Test
    fun aFallbackOutageReportsTheDocumentAsUnsupported() = rasoiTest(
        fallback = InvoiceFallback { AppResult.Failure(AppError.Timeout) },
    ) {
        val owner = signIn("+919812345678", "Meera")
        val home = household(owner)
        val response = upload(
            owner,
            home.id,
            Invoices.upload(
                Invoices.pdf(
                    "zepto",
                    "ZEPTO" to "ACME",
                    "Geddit" to "Acme",
                ),
            ),
        )
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(
            listOf("documents[0]" to "unsupported_format"),
            response.error().fields.map {
                it.field to it.code
            },
        )
    }

    @Test
    fun aStorageFailureSavesNothing() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val home = household(owner)
        storage.failWrites = true
        assertEquals(
            HttpStatusCode.InternalServerError,
            upload(owner, home.id, Invoices.upload(Invoices.pdf("zepto"))).status,
        )
        storage.failWrites = false
        assertEquals(HttpStatusCode.Created, upload(owner, home.id, Invoices.upload(Invoices.pdf("zepto"))).status)
    }

    @Test
    fun retriesReplayTheFirstResponse() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val home = household(owner)
        val body = Invoices.upload(Invoices.pdf("instamart"))
        val first: BillDto = upload(owner, home.id, body, key = "upload-key-1").body()
        val retry = upload(owner, home.id, body, key = "upload-key-1")
        assertEquals(HttpStatusCode.Created, retry.status)
        assertEquals(first.id, retry.body<BillDto>().id)
        assertEquals(HttpStatusCode.BadRequest, upload(owner, home.id, body, key = null).status)
    }

    @Test
    fun onlyMembersWhoMayEditBillsCanImportAndDraftsStayPrivate() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val flatmate = signIn("+919800000001", "Aarav")
        val cook = signIn("+919800000002", "Lakshmi")
        val outsider = signIn("+919800000099", "Eve")
        val home = household(owner)
        join(owner, home.id, flatmate, RoleDto.MEMBER)
        join(owner, home.id, cook, RoleDto.COOK)
        val pdf = Invoices.upload(Invoices.pdf("zepto"))

        assertEquals(HttpStatusCode.NotFound, upload(outsider, home.id, pdf).status)
        assertEquals(HttpStatusCode.Forbidden, upload(cook, home.id, pdf).status)
        assertEquals(HttpStatusCode.NotFound, upload(owner, "not-a-uuid", pdf).status)
        val bill: BillDto = upload(owner, home.id, pdf).body()
        listOf(flatmate, cook, outsider).forEach { other ->
            assertEquals(HttpStatusCode.NotFound, client.get("/bills/${bill.id}") { authed(other, null) }.status)
            assertEquals(HttpStatusCode.NotFound, edit(other, bill.id, UpdateBillRequestDto(totalPaise = 1)).status)
        }
        assertEquals(HttpStatusCode.NotFound, client.get("/bills/nope") { authed(owner, null) }.status)
    }

    @Test
    fun editingADraftResolvesAMismatchAndKeepsLineIds() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val home = household(owner)
        val tampered = Invoices.pdf("zepto", "867.00" to "868.00")
        val bill: BillDto = upload(owner, home.id, Invoices.upload(tampered)).body()
        assertEquals(listOf(BillWarningDto(BillWarningCodeDto.TOTAL_MISMATCH, 86_700, 86_800)), bill.warnings)

        val fixed: BillDto = edit(owner, bill.id, UpdateBillRequestDto(totalPaise = 86_700)).body()
        assertEquals(emptyList(), fixed.warnings)

        val first = bill.lines.first()
        val lines = listOf(BillLineEditDto(first.id, "Potatoes", 2, first.amountPaise)) +
            bill.lines.drop(1).map { BillLineEditDto(it.id, it.displayName, it.units, it.amountPaise) } +
            BillLineEditDto(null, "Carry bag", 1, 500)
        val charges = bill.charges.map { BillChargeEditDto(it.id, it.kind, it.label, it.amountPaise) } +
            BillChargeEditDto(null, ChargeKindDto.DISCOUNT, "Coupon", -500)
        val edited: BillDto = edit(owner, bill.id, UpdateBillRequestDto(lines = lines, charges = charges)).body()
        assertEquals(first.id, edited.lines.first().id)
        assertEquals("Potatoes" to first.rawName, edited.lines.first().displayName to edited.lines.first().rawName)
        assertEquals("Carry bag", edited.lines.last().displayName)
        assertEquals(bill.charges.map { it.label } + "Coupon", edited.charges.map { it.label })
        assertEquals(emptyList(), edited.warnings)
        assertEquals(edited, client.get("/bills/${bill.id}") { authed(owner, null) }.body<BillDto>())

        val removed: BillDto = edit(owner, bill.id, UpdateBillRequestDto(lines = lines.take(1))).body()
        assertEquals(listOf(first.id), removed.lines.map { it.id })
    }

    @Test
    fun invalidEditsReportFieldsAndOversizedBodiesAreRejected() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val home = household(owner)
        val bill: BillDto = upload(owner, home.id, Invoices.upload(Invoices.pdf("handpickd"))).body()
        val blank = edit(owner, bill.id, UpdateBillRequestDto(lines = listOf(BillLineEditDto(null, " ", 1, 100))))
        assertEquals(listOf("lines[0].name" to "required"), blank.error().fields.map { it.field to it.code })
        val foreign = edit(owner, bill.id, UpdateBillRequestDto(lines = listOf(BillLineEditDto("x", "Tea", 1, 100))))
        assertEquals("lines[0].id", foreign.error().fields.single().field)
        assertEquals("empty", edit(owner, bill.id, UpdateBillRequestDto()).error().fields.single().code)
        val huge = UpdateBillRequestDto(lines = List(2_000) { BillLineEditDto(null, "Item number $it", 1, 100) })
        assertEquals("too_large", edit(owner, bill.id, huge).error().fields.single().code)
    }
}
