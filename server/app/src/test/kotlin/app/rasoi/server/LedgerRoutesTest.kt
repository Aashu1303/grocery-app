package app.rasoi.server

import app.rasoi.core.contract.ErrorDto
import app.rasoi.core.contract.auth.SessionDto
import app.rasoi.core.contract.auth.UpdateProfileRequestDto
import app.rasoi.core.contract.auth.UserDto
import app.rasoi.core.contract.bills.ApprovalStateDto
import app.rasoi.core.contract.bills.BillDto
import app.rasoi.core.contract.bills.BillLineEditDto
import app.rasoi.core.contract.bills.BillStatusDto
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
import app.rasoi.core.contract.ledger.LedgerDto
import app.rasoi.core.contract.ledger.PeriodStatusDto
import app.rasoi.core.contract.ledger.SettlementStatusDto
import app.rasoi.core.contract.ledger.TransferDto
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
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class LedgerRoutesTest {
    private class Flat(
        val home: HouseholdDto,
        val meera: SessionDto,
        val dev: SessionDto,
        val asha: SessionDto,
        val cook: SessionDto,
    )

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

    /** Meera (owner), Dev and Asha (members) and a cook share a flat. */
    private suspend fun TestScope.flat(): Flat {
        val meera = signIn("+919812345678", "Meera")
        val dev = signIn("+919812345679", "Dev")
        val asha = signIn("+919812345680", "Asha")
        val cook = signIn("+919812345681", "Lakshmi")
        val home: HouseholdDto = client.post("/households") {
            authed(meera)
            setBody(CreateHouseholdRequestDto("Flat 4B", "Asia/Kolkata"))
        }.body()
        join(meera, home.id, dev, RoleDto.MEMBER)
        join(meera, home.id, asha, RoleDto.MEMBER)
        join(meera, home.id, cook, RoleDto.COOK)
        return Flat(home, meera, dev, asha, cook)
    }

    private suspend fun TestScope.post(session: SessionDto, path: String, body: Any? = null): HttpResponse =
        client.post(path) {
            authed(session)
            setBody(body ?: "{}")
        }

    /** [payer] buys [amount] paise of something shared equally by the people in [sharers]; returns the draft. */
    private suspend fun TestScope.bill(
        flat: Flat,
        payer: SessionDto,
        amount: Long,
        sharers: List<SessionDto>,
    ): BillDto {
        val created: BillDto = client.post("/households/${flat.home.id}/bills") {
            authed(payer)
            setBody(
                CreateBillRequestDto(
                    title = "Shared $amount",
                    lines = listOf(BillLineEditDto(displayName = "Goods", units = 1, amountPaise = amount)),
                ),
            )
        }.body()
        return client.patch("/bills/${created.id}") {
            authed(payer)
            setBody(
                UpdateBillRequestDto(
                    splits = listOf(
                        LineSplitEditDto(
                            created.lines.single().id,
                            SplitModeDto.EQUAL,
                            sharers.map { SplitEntryDto(it.user.id) },
                        ),
                    ),
                ),
            )
        }.body()
    }

    /** Submits [bill] and has everyone who owes approve it. */
    private suspend fun TestScope.approved(bill: BillDto, payer: SessionDto, approvers: List<SessionDto>): BillDto {
        post(payer, "/bills/${bill.id}/submit")
        approvers.forEach {
            post(it, "/bills/${bill.id}/approvals", DecideApprovalRequestDto(ApprovalStateDto.APPROVED))
        }
        return client.get("/bills/${bill.id}") { authed(payer) }.body()
    }

    private suspend fun TestScope.ledger(session: SessionDto, flat: Flat, period: String? = null): LedgerDto =
        client.get("/households/${flat.home.id}/ledger" + period?.let { "?period=$it" }.orEmpty()) {
            authed(session)
        }.body()

    private fun LedgerDto.net() = balances.associate { it.userId to it.netPaise }

    @Test
    fun anEmptyHouseholdHasAnOpenPeriodForThisMonthAndNothingToSettle() = rasoiTest {
        val flat = flat()
        val ledger = ledger(flat.meera, flat)
        assertEquals("2026-10", ledger.month)
        assertEquals(PeriodStatusDto.OPEN, ledger.status)
        assertEquals(emptyList(), ledger.balances)
        assertEquals(emptyList(), ledger.transfers)
        assertEquals(listOf("2026-10"), ledger.months)
    }

    @Test
    fun onlyApprovedBillsShapeTheBalancesAndTheRestAreCounted() = rasoiTest {
        val flat = flat()
        approved(bill(flat, flat.meera, 30_000, listOf(flat.meera, flat.dev)), flat.meera, listOf(flat.dev))
        val pending = bill(flat, flat.asha, 9_000, listOf(flat.asha, flat.dev))
        post(flat.asha, "/bills/${pending.id}/submit")
        bill(flat, flat.dev, 5_000, listOf(flat.dev, flat.meera))

        val ledger = ledger(flat.dev, flat)
        assertEquals(mapOf(flat.meera.user.id to 15_000L, flat.dev.user.id to -15_000L), ledger.net())
        assertEquals(1, ledger.pendingBills)
        assertEquals(
            listOf(Triple(flat.dev.user.id, flat.meera.user.id, 15_000L)),
            ledger.transfers.map { Triple(it.from, it.to, it.amountPaise) },
        )
        assertNull(ledger.transfers.single().settlementId)
    }

    @Test
    fun suggestedTransfersCarryThePayeesUpiAddress() = rasoiTest {
        val flat = flat()
        approved(bill(flat, flat.meera, 30_000, listOf(flat.meera, flat.dev)), flat.meera, listOf(flat.dev))
        assertNull(ledger(flat.dev, flat).transfers.single().payeeUpiVpa)

        val me: UserDto = client.patch("/me") {
            authed(flat.meera)
            setBody(UpdateProfileRequestDto("meera@okbank"))
        }.body()
        assertEquals("meera@okbank", me.upiVpa)
        assertEquals("meera@okbank", ledger(flat.dev, flat).transfers.single().payeeUpiVpa)
    }

    @Test
    fun theUpiAddressIsValidatedAndCanBeCleared() = rasoiTest {
        val flat = flat()
        suspend fun set(vpa: String?) = client.patch("/me") {
            authed(flat.meera)
            setBody(UpdateProfileRequestDto(vpa))
        }
        assertEquals(HttpStatusCode.BadRequest, set("not a vpa").status)
        assertEquals(HttpStatusCode.OK, set("  meera@okbank  ").status)
        assertEquals(
            "meera@okbank",
            client.get("/me") {
                authed(flat.meera)
            }.body<app.rasoi.core.contract.auth.MeDto>().user.upiVpa,
        )
        assertEquals(null, set("").body<UserDto>().upiVpa)
    }

    @Test
    fun theLedgerIsForMembersWhoMayViewMoney() = rasoiTest {
        val flat = flat()
        val stranger = signIn("+919800000000", "Stranger")
        val path = "/households/${flat.home.id}/ledger"
        assertEquals(HttpStatusCode.Forbidden, client.get(path) { authed(flat.cook) }.status)
        assertEquals(HttpStatusCode.NotFound, client.get(path) { authed(stranger) }.status)
        assertEquals(HttpStatusCode.BadRequest, client.get("$path?period=October") { authed(flat.meera) }.status)
        assertEquals(HttpStatusCode.NotFound, client.get("$path?period=2025-01") { authed(flat.meera) }.status)
    }

    @Test
    fun anOwnerClosesTheMonthEarlyAndTheTransfersAreStored() = rasoiTest {
        val flat = flat()
        val gas =
            approved(
                bill(flat, flat.meera, 30_000, listOf(flat.meera, flat.dev, flat.asha)),
                flat.meera,
                listOf(flat.dev, flat.asha),
            )
        val waiting = bill(flat, flat.dev, 6_000, listOf(flat.dev, flat.asha))
        post(flat.dev, "/bills/${waiting.id}/submit")
        val draft = bill(flat, flat.asha, 3_000, listOf(flat.asha, flat.dev))

        val path = "/households/${flat.home.id}/periods/2026-10/close"
        assertEquals(HttpStatusCode.Forbidden, post(flat.dev, path).status)
        assertEquals(HttpStatusCode.Forbidden, post(flat.cook, path).status)
        assertEquals(
            HttpStatusCode.Conflict,
            post(flat.meera, "/households/${flat.home.id}/periods/2026-09/close").status,
        )
        val closed: LedgerDto = post(flat.meera, path).body()

        assertEquals("2026-10", closed.month)
        assertEquals(PeriodStatusDto.LOCKED, closed.status)
        assertEquals(
            mapOf(flat.meera.user.id to 20_000L, flat.dev.user.id to -10_000L, flat.asha.user.id to -10_000L),
            closed.net(),
        )
        assertEquals(listOf(10_000L, 10_000L), closed.transfers.map { it.amountPaise })
        assertEquals(setOf(SettlementStatusDto.PENDING), closed.transfers.map { it.status }.toSet())
        assertEquals(setOf(flat.meera.user.id), closed.transfers.map { it.to }.toSet())

        // The approved bill is locked; the others carry on in the next month.
        assertEquals(BillStatusDto.LOCKED, client.get("/bills/${gas.id}") { authed(flat.meera) }.body<BillDto>().status)
        assertEquals(
            HttpStatusCode.Conflict,
            post(flat.dev, "/bills/${gas.id}/approvals", DecideApprovalRequestDto(ApprovalStateDto.APPROVED)).status,
        )
        val edit = client.patch("/bills/${gas.id}") {
            authed(flat.meera)
            setBody(UpdateBillRequestDto(payerId = flat.dev.user.id))
        }
        assertEquals("bill_not_editable", edit.body<ErrorDto>().code)

        val next = ledger(flat.meera, flat)
        assertEquals("2026-11", next.month)
        assertEquals(PeriodStatusDto.OPEN, next.status)
        assertEquals(1, next.pendingBills)
        assertEquals(listOf("2026-11", "2026-10"), next.months)
        assertEquals(BillStatusDto.DRAFT, client.get("/bills/${draft.id}") { authed(flat.asha) }.body<BillDto>().status)
        assertEquals(closed, ledger(flat.meera, flat, "2026-10"))

        // The closed month cannot be closed twice, and new bills join the new period.
        assertEquals(HttpStatusCode.Conflict, post(flat.meera, path).status)
    }

    @Test
    fun eitherSideOfATransferOrAnOwnerMarksItPaid() = rasoiTest {
        val flat = flat()
        approved(
            bill(flat, flat.meera, 30_000, listOf(flat.meera, flat.dev, flat.asha)),
            flat.meera,
            listOf(flat.dev, flat.asha),
        )
        val closed: LedgerDto = post(flat.meera, "/households/${flat.home.id}/periods/2026-10/close").body()
        val toDev = closed.transfers.first { it.from == flat.dev.user.id }
        val toAsha = closed.transfers.first { it.from == flat.asha.user.id }

        assertEquals(HttpStatusCode.Forbidden, post(flat.asha, "/settlements/${toDev.settlementId}/paid").status)
        assertEquals(HttpStatusCode.Forbidden, post(flat.cook, "/settlements/${toDev.settlementId}/paid").status)
        val stranger = signIn("+919800000000", "Stranger")
        assertEquals(HttpStatusCode.NotFound, post(stranger, "/settlements/${toDev.settlementId}/paid").status)
        assertEquals(
            HttpStatusCode.NotFound,
            post(flat.dev, "/settlements/00000000-0000-0000-0000-000000000000/paid").status,
        )

        val byPayer: TransferDto = post(flat.dev, "/settlements/${toDev.settlementId}/paid").body()
        assertEquals(SettlementStatusDto.PAID, byPayer.status)
        assertEquals(
            SettlementStatusDto.PAID,
            post(flat.dev, "/settlements/${toDev.settlementId}/paid").body<TransferDto>().status,
        )
        assertEquals(
            SettlementStatusDto.PAID,
            post(flat.meera, "/settlements/${toAsha.settlementId}/paid").body<TransferDto>().status,
        )

        val statuses = ledger(flat.meera, flat, "2026-10").transfers.map { it.status }
        assertEquals(listOf(SettlementStatusDto.PAID, SettlementStatusDto.PAID), statuses)
    }

    @Test
    fun theMonthClosesByItselfOnceItEndsInTheHouseholdsTimeZone() = rasoiTest {
        val flat = flat()
        approved(bill(flat, flat.meera, 30_000, listOf(flat.meera, flat.dev)), flat.meera, listOf(flat.dev))

        // 23:30 on 31 October in Kolkata: still October.
        clock.advance(20.days + 13.hours + 30.minutes)
        assertEquals(0, graph.periods.closeDue())
        assertEquals(PeriodStatusDto.OPEN, ledger(flat.meera, flat).status)

        // 00:30 on 1 November in Kolkata.
        clock.advance(1.hours)
        assertEquals(1, graph.periods.closeDue())
        assertEquals(0, graph.periods.closeDue())
        val october = ledger(flat.meera, flat, "2026-10")
        assertEquals(PeriodStatusDto.LOCKED, october.status)
        assertEquals(listOf(15_000L), october.transfers.map { it.amountPaise })
        assertEquals("2026-11", ledger(flat.meera, flat).month)
    }

    @Test
    fun aLateCloseOpensTheMonthWeAreIn() = rasoiTest {
        val flat = flat()
        approved(bill(flat, flat.meera, 30_000, listOf(flat.meera, flat.dev)), flat.meera, listOf(flat.dev))
        clock.advance(60.days)
        assertEquals(1, graph.periods.closeDue())
        // Sessions last 30 days, so Meera signs in again.
        val meera = signIn("+919812345678", "Meera")
        assertEquals("2026-12", ledger(meera, flat).month)
    }
}
