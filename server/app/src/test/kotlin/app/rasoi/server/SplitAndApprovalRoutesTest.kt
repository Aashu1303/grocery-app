package app.rasoi.server

import app.rasoi.core.contract.ErrorDto
import app.rasoi.core.contract.auth.SessionDto
import app.rasoi.core.contract.bills.ApprovalStateDto
import app.rasoi.core.contract.bills.BillChargeEditDto
import app.rasoi.core.contract.bills.BillDto
import app.rasoi.core.contract.bills.BillLineEditDto
import app.rasoi.core.contract.bills.BillStatusDto
import app.rasoi.core.contract.bills.BillSummaryDto
import app.rasoi.core.contract.bills.ChargeDistributionDto
import app.rasoi.core.contract.bills.ChargeKindDto
import app.rasoi.core.contract.bills.CreateBillRequestDto
import app.rasoi.core.contract.bills.DecideApprovalRequestDto
import app.rasoi.core.contract.bills.LineSplitEditDto
import app.rasoi.core.contract.bills.SplitEntryDto
import app.rasoi.core.contract.bills.SplitModeDto
import app.rasoi.core.contract.bills.UpdateBillRequestDto
import app.rasoi.core.contract.household.CreateHouseholdRequestDto
import app.rasoi.core.contract.household.CreateInviteRequestDto
import app.rasoi.core.contract.household.HouseholdDto
import app.rasoi.core.contract.household.InviteDto
import app.rasoi.core.contract.household.RoleDto
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.BillId
import app.rasoi.core.model.UserId
import app.rasoi.server.persistence.BillStore
import app.rasoi.server.persistence.PantryChanges
import app.rasoi.server.support.TestPostgres
import app.rasoi.server.support.TestScope
import app.rasoi.server.support.authed
import app.rasoi.server.support.rasoiTest
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class SplitAndApprovalRoutesTest {
    private class Flat(
        val home: HouseholdDto,
        val meera: SessionDto,
        val dev: SessionDto,
        val asha: SessionDto,
        val bill: BillDto,
    ) {
        val rice get() = bill.lines.first { it.displayName == "Rice" }.id
        val chips get() = bill.lines.first { it.displayName == "Chips" }.id
    }

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

    /** Three flatmates and Meera's draft of rice (30000) and chips (6000); nothing is split yet. */
    private suspend fun TestScope.flat(charges: List<BillChargeEditDto> = emptyList()): Flat {
        val meera = signIn("+919812345678", "Meera")
        val dev = signIn("+919812345679", "Dev")
        val asha = signIn("+919812345680", "Asha")
        val home: HouseholdDto = client.post("/households") {
            authed(meera)
            setBody(CreateHouseholdRequestDto("Flat 4B", "Asia/Kolkata"))
        }.body()
        join(meera, home.id, dev, RoleDto.MEMBER)
        join(meera, home.id, asha, RoleDto.MEMBER)
        val bill: BillDto = client.post("/households/${home.id}/bills") {
            authed(meera)
            setBody(
                CreateBillRequestDto(
                    title = "Weekly groceries",
                    invoiceDate = "2026-10-03",
                    lines = listOf(
                        BillLineEditDto(displayName = "Rice", units = 1, amountPaise = 30_000),
                        BillLineEditDto(displayName = "Chips", units = 1, amountPaise = 6_000),
                    ),
                    charges = charges,
                ),
            )
        }.body()
        return Flat(home, meera, dev, asha, bill)
    }

    private fun Flat.everyone() = listOf(meera, dev, asha).map { SplitEntryDto(it.user.id) }

    private suspend fun TestScope.split(flat: Flat, session: SessionDto = flat.meera): BillDto = patch(
        session,
        flat.bill.id,
        UpdateBillRequestDto(
            splits = listOf(
                LineSplitEditDto(flat.rice, SplitModeDto.EQUAL, flat.everyone()),
                LineSplitEditDto(flat.chips, SplitModeDto.ONE, listOf(SplitEntryDto(flat.dev.user.id))),
            ),
        ),
    ).body()

    private suspend fun TestScope.patch(session: SessionDto, billId: String, body: UpdateBillRequestDto) =
        client.patch("/bills/$billId") {
            authed(session)
            setBody(body)
        }

    private suspend fun TestScope.submit(session: SessionDto, billId: String): HttpResponse =
        client.post("/bills/$billId/submit") {
            authed(session)
            setBody("{}")
        }

    private suspend fun TestScope.decide(
        session: SessionDto,
        billId: String,
        state: ApprovalStateDto,
        comment: String? = null,
    ): HttpResponse = client.post("/bills/$billId/approvals") {
        authed(session)
        setBody(DecideApprovalRequestDto(state, comment))
    }

    private suspend fun TestScope.fetch(session: SessionDto, billId: String): BillDto =
        client.get("/bills/$billId") { authed(session) }.body()

    private suspend fun HttpResponse.error(): ErrorDto = body()

    @Test
    fun aHandTypedBillIsADraftOnlyItsCreatorSeesInTheList() = rasoiTest {
        val flat = flat()
        assertEquals(BillStatusDto.DRAFT, flat.bill.status)
        assertEquals("Weekly groceries", flat.bill.title)
        assertEquals(36_000, flat.bill.totalPaise)
        assertEquals(emptyList(), flat.bill.warnings)
        val own: List<BillSummaryDto> = client.get("/households/${flat.home.id}/bills") { authed(flat.meera) }.body()
        assertEquals(listOf(flat.bill.id), own.map { it.id })
        assertEquals(2, own.single().lineCount)
        val others: List<BillSummaryDto> = client.get("/households/${flat.home.id}/bills") { authed(flat.dev) }.body()
        assertEquals(emptyList(), others)
        assertEquals(HttpStatusCode.NotFound, client.get("/bills/${flat.bill.id}") { authed(flat.dev) }.status)
    }

    @Test
    fun aHandTypedBillWithoutADateIsDatedTodayInTheHouseholdZone() = rasoiTest {
        val flat = flat()
        clock.advance(14.hours + 30.minutes)
        val bill: BillDto = client.post("/households/${flat.home.id}/bills") {
            authed(flat.meera)
            setBody(
                CreateBillRequestDto(
                    title = "Late night cylinder",
                    lines = listOf(BillLineEditDto(displayName = "Cylinder", units = 1, amountPaise = 95_000)),
                ),
            )
        }.body()
        // 19:00 UTC on the 11th is already 00:30 on the 12th in Kolkata.
        assertEquals("2026-10-12", bill.invoiceDate)
    }

    @Test
    fun aHandTypedBillIsValidatedAndMustNameMembers() = rasoiTest {
        val flat = flat()
        val stranger = signIn("+919800000000", "Stranger")
        fun request(payer: String? = null, date: String = "2026-10-03", title: String = "Gas") = CreateBillRequestDto(
            title = title,
            invoiceDate = date,
            lines = listOf(BillLineEditDto(displayName = "Cylinder", units = 1, amountPaise = 95_000)),
            payerId = payer,
        )
        suspend fun create(session: SessionDto, body: CreateBillRequestDto) =
            client.post("/households/${flat.home.id}/bills") {
                authed(session)
                setBody(body)
            }
        assertEquals(HttpStatusCode.BadRequest, create(flat.meera, request(date = "yesterday")).status)
        assertEquals(HttpStatusCode.BadRequest, create(flat.meera, request(title = " ")).status)
        assertEquals(HttpStatusCode.BadRequest, create(flat.meera, request(payer = stranger.user.id)).status)
        assertEquals(HttpStatusCode.NotFound, create(stranger, request()).status)
        assertEquals(HttpStatusCode.Created, create(flat.dev, request(payer = flat.asha.user.id)).status)
    }

    @Test
    fun splittingAssignsEachLineAndSharesAddUpToTheTotal() = rasoiTest {
        val flat = flat(
            charges = listOf(BillChargeEditDto(kind = ChargeKindDto.DELIVERY, label = "Delivery", amountPaise = 3_000)),
        )
        val split = split(flat)
        assertEquals(
            mapOf(flat.meera.user.id to 10_000L, flat.dev.user.id to 16_000L, flat.asha.user.id to 10_000L),
            split.allocations.groupBy { it.userId }.mapValues { (_, rows) -> rows.sumOf { it.amountPaise } },
        )
        assertEquals(36_000L + 3_000L, split.shares.sumOf { it.totalPaise })
        assertEquals(
            setOf(flat.meera.user.id, flat.dev.user.id, flat.asha.user.id),
            split.shares.map {
                it.userId
            }.toSet(),
        )
        // Delivery is shared in proportion to lines: 10000 : 16000 : 10000 of 3000 is 833.3 : 1333.3 : 833.3.
        assertEquals(3_000L, split.shares.sumOf { it.chargesPaise })
        assertEquals(ChargeDistributionDto.PROPORTIONAL, split.charges.single().distribution)
    }

    @Test
    fun aChargeCanBeSharedEqually() = rasoiTest {
        val flat = flat(
            charges = listOf(
                BillChargeEditDto(
                    kind = ChargeKindDto.DELIVERY,
                    label = "Delivery",
                    amountPaise = 3_000,
                    distribution = ChargeDistributionDto.EQUAL,
                ),
            ),
        )
        val split = split(flat)
        assertEquals(listOf(1_000L, 1_000L, 1_000L), split.shares.map { it.chargesPaise })
    }

    @Test
    fun invalidSplitsAreRejectedByPosition() = rasoiTest {
        val flat = flat()
        val stranger = signIn("+919800000000", "Stranger")
        suspend fun attempt(splits: List<LineSplitEditDto>) =
            patch(flat.meera, flat.bill.id, UpdateBillRequestDto(splits = splits))
        val unknownLine = attempt(listOf(LineSplitEditDto("nope", SplitModeDto.EQUAL, flat.everyone())))
        assertEquals(HttpStatusCode.BadRequest, unknownLine.status)
        val cook = signIn("+919800000001", "Lakshmi")
        join(flat.meera, flat.home.id, cook, RoleDto.COOK)
        val cooksShare = attempt(
            listOf(LineSplitEditDto(flat.rice, SplitModeDto.ONE, listOf(SplitEntryDto(cook.user.id)))),
        )
        assertEquals(HttpStatusCode.BadRequest, cooksShare.status)
        val outsider = attempt(
            listOf(LineSplitEditDto(flat.rice, SplitModeDto.ONE, listOf(SplitEntryDto(stranger.user.id)))),
        )
        assertEquals(HttpStatusCode.BadRequest, outsider.status)
        val mismatched = attempt(
            listOf(LineSplitEditDto(flat.rice, SplitModeDto.QUANTITY, listOf(SplitEntryDto(flat.dev.user.id, 3)))),
        )
        assertEquals(HttpStatusCode.BadRequest, mismatched.status)
        assertEquals(emptyList(), fetch(flat.meera, flat.bill.id).allocations)
    }

    @Test
    fun aBillMustBeFullySplitBeforeItCanBeSubmitted() = rasoiTest {
        val flat = flat()
        val response = submit(flat.meera, flat.bill.id)
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(
            listOf("lines[0].split", "lines[1].split"),
            response.error().fields.map { it.field },
        )
    }

    @Test
    fun submittingAsksEveryMemberWhoOwesExceptAnUploadingPayer() = rasoiTest {
        val flat = flat()
        split(flat)
        val submitted: BillDto = submit(flat.meera, flat.bill.id).body()
        assertEquals(BillStatusDto.PENDING, submitted.status)
        assertEquals(setOf(flat.dev.user.id, flat.asha.user.id), submitted.approvals.map { it.userId }.toSet())
        assertEquals(setOf(ApprovalStateDto.PENDING), submitted.approvals.map { it.state }.toSet())
        assertEquals(clockNow(), submitted.submittedAt)
        // Now visible to the whole flat.
        assertEquals(BillStatusDto.PENDING, fetch(flat.asha, flat.bill.id).status)
        assertEquals(HttpStatusCode.Forbidden, submit(flat.dev, flat.bill.id).status)
    }

    private fun TestScope.clockNow() = clock.now()

    @Test
    fun approvingEverywhereApprovesTheBillAndAChangeRequestNeedsAReason() = rasoiTest {
        val flat = flat()
        split(flat)
        submit(flat.meera, flat.bill.id)
        assertEquals(
            HttpStatusCode.BadRequest,
            decide(flat.asha, flat.bill.id, ApprovalStateDto.CHANGES_REQUESTED).status,
        )
        assertEquals(HttpStatusCode.BadRequest, decide(flat.asha, flat.bill.id, ApprovalStateDto.PENDING).status)
        val afterDev: BillDto = decide(flat.dev, flat.bill.id, ApprovalStateDto.APPROVED).body()
        assertEquals(BillStatusDto.PENDING, afterDev.status)
        val afterAsha: BillDto = decide(flat.asha, flat.bill.id, ApprovalStateDto.APPROVED).body()
        assertEquals(BillStatusDto.APPROVED, afterAsha.status)
        assertEquals(HttpStatusCode.Conflict, decide(flat.asha, flat.bill.id, ApprovalStateDto.APPROVED).status)
    }

    @Test
    fun aChangeRequestHoldsTheBillUntilTheUploaderAnswersIt() = rasoiTest {
        val flat = flat()
        split(flat)
        submit(flat.meera, flat.bill.id)
        val requested: BillDto = decide(
            flat.dev,
            flat.bill.id,
            ApprovalStateDto.CHANGES_REQUESTED,
            "Chips were mine only on the 3rd",
        ).body()
        assertEquals(BillStatusDto.CHANGES_REQUESTED, requested.status)
        assertEquals(
            "Chips were mine only on the 3rd",
            requested.approvals.first { it.userId == flat.dev.user.id }.comment,
        )
        val resubmitted: BillDto = submit(flat.meera, flat.bill.id).body()
        assertEquals(BillStatusDto.PENDING, resubmitted.status)
        assertEquals(
            ApprovalStateDto.PENDING,
            resubmitted.approvals.first { it.userId == flat.dev.user.id }.state,
        )
    }

    @Test
    fun editingASubmittedBillAsksAgainOnlyThoseWhoseShareMoved() = rasoiTest {
        val flat = flat()
        split(flat)
        submit(flat.meera, flat.bill.id)
        decide(flat.dev, flat.bill.id, ApprovalStateDto.APPROVED)
        decide(flat.asha, flat.bill.id, ApprovalStateDto.APPROVED)
        val edited: BillDto = patch(
            flat.meera,
            flat.bill.id,
            UpdateBillRequestDto(
                lines = listOf(
                    BillLineEditDto(flat.rice, "Rice", 1, 30_000),
                    BillLineEditDto(flat.chips, "Chips", 1, 9_000),
                ),
                totalPaise = 39_000,
            ),
        ).body()
        assertEquals(BillStatusDto.PENDING, edited.status)
        val states = edited.approvals.associate { it.userId to it.state }
        assertEquals(ApprovalStateDto.PENDING, states.getValue(flat.dev.user.id))
        assertEquals(ApprovalStateDto.APPROVED, states.getValue(flat.asha.user.id))
        assertEquals(16_000L + 3_000L, edited.shares.first { it.userId == flat.dev.user.id }.totalPaise)
    }

    @Test
    fun changingThePayerAfterSubmissionIsAllowedAndKeepsTheBillSplit() = rasoiTest {
        val flat = flat()
        split(flat)
        submit(flat.meera, flat.bill.id)
        val edited: BillDto = patch(flat.meera, flat.bill.id, UpdateBillRequestDto(payerId = flat.dev.user.id)).body()
        assertEquals(flat.dev.user.id, edited.payerId)
        assertEquals(4, edited.allocations.size)
        assertEquals(
            ApprovalStateDto.PENDING,
            edited.approvals.first { it.userId == flat.meera.user.id }.state,
        )
    }

    @Test
    fun onlyTheUploaderOrAnOwnerEditsASubmittedBillAndOnlyThoseWithAShareDecide() = rasoiTest {
        val flat = flat()
        val cook = signIn("+919800000001", "Lakshmi")
        join(flat.meera, flat.home.id, cook, RoleDto.COOK)
        split(flat)
        submit(flat.meera, flat.bill.id)
        val change = UpdateBillRequestDto(payerId = flat.asha.user.id)
        assertEquals(HttpStatusCode.Forbidden, patch(flat.dev, flat.bill.id, change).status)
        assertEquals(HttpStatusCode.Forbidden, decide(flat.meera, flat.bill.id, ApprovalStateDto.APPROVED).status)
        assertEquals(HttpStatusCode.Forbidden, decide(cook, flat.bill.id, ApprovalStateDto.APPROVED).status)
        assertEquals(HttpStatusCode.Forbidden, client.get("/bills/${flat.bill.id}") { authed(cook) }.status)
        val stranger = signIn("+919800000000", "Stranger")
        assertEquals(HttpStatusCode.NotFound, decide(stranger, flat.bill.id, ApprovalStateDto.APPROVED).status)
        assertEquals(HttpStatusCode.NotFound, submit(stranger, flat.bill.id).status)
    }

    @Test
    fun silenceForFortyEightHoursApprovesTheBill() = rasoiTest {
        val flat = flat()
        split(flat)
        submit(flat.meera, flat.bill.id)
        decide(flat.dev, flat.bill.id, ApprovalStateDto.APPROVED)
        clock.advance(47.hours)
        assertEquals(0, graph.bills.autoApproveDue())
        assertEquals(BillStatusDto.PENDING, fetch(flat.meera, flat.bill.id).status)
        clock.advance(2.hours)
        assertEquals(1, graph.bills.autoApproveDue())
        val approved = fetch(flat.meera, flat.bill.id)
        assertEquals(BillStatusDto.APPROVED, approved.status)
        val asha = approved.approvals.first { it.userId == flat.asha.user.id }
        assertEquals(true, asha.automatic)
        assertEquals(false, approved.approvals.first { it.userId == flat.dev.user.id }.automatic)
        assertEquals(0, graph.bills.autoApproveDue())
    }

    @Test
    fun theSchedulerSweepApprovesASilentBill() = rasoiTest(sweepToken = SweepToken("s".repeat(32))) {
        val flat = flat()
        split(flat)
        submit(flat.meera, flat.bill.id)
        decide(flat.dev, flat.bill.id, ApprovalStateDto.APPROVED)
        clock.advance(49.hours)
        val sweep = client.post("/internal/sweeps") { header(SweepToken.HEADER, "s".repeat(32)) }
        assertEquals(HttpStatusCode.NoContent, sweep.status)
        assertEquals(BillStatusDto.APPROVED, fetch(flat.meera, flat.bill.id).status)
    }

    @Test
    fun aSaveBasedOnAnOutOfDateReadIsRejectedAndWritesNothing() = rasoiTest {
        val flat = flat()
        split(flat)
        val store = BillStore(TestPostgres.database)
        val read = requireNotNull(store.find(BillId(flat.bill.id)))
        val first = store.save(read.copy(payerId = UserId(flat.dev.user.id)), PantryChanges(), clock.now())
        assertEquals(read.version + 1, (first as AppResult.Success).value.version)
        val stale = store.save(read.copy(payerId = UserId(flat.asha.user.id)), PantryChanges(), clock.now())
        assertEquals(AppResult.Failure(AppError.Conflict("bill_changed")), stale)
        assertEquals(flat.dev.user.id, fetch(flat.meera, flat.bill.id).payerId)
    }
}
