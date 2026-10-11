package app.rasoi.core.domain.split

import app.rasoi.core.model.Allocation
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.ApprovalState
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillLine
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.LineSplit
import app.rasoi.core.model.Money
import app.rasoi.core.model.SplitEntry
import app.rasoi.core.model.SplitMode
import app.rasoi.core.model.UserId
import app.rasoi.core.testing.Builders
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class ApprovalRulesTest {
    private val meera = UserId("u1")
    private val aarav = UserId("u2")
    private val lakshmi = UserId("u3")
    private val t0 = Instant.parse("2026-10-11T10:00:00Z")
    private val rice = Builders.line("a", "Rice", 30_000)
    private val milk = Builders.line("b", "Milk", 10_000)

    private fun split(line: BillLine, mode: SplitMode, first: UserId, second: UserId? = null): List<Allocation> {
        val entries = listOfNotNull(first, second).map { SplitEntry(it) }
        return assertIs<AppResult.Success<List<Allocation>>>(
            LineAllocator.allocate(line, LineSplit(mode, entries)),
        ).value
    }

    /** Meera uploaded and paid; she keeps the rice, Aarav and Lakshmi share the milk. */
    private fun draft(): Bill = Builders.bill(lines = listOf(rice, milk)).copy(
        payerId = meera,
        createdBy = meera,
        allocations = split(rice, SplitMode.ONE, meera) + split(milk, SplitMode.EQUAL, aarav, lakshmi),
    )

    private fun ok(result: AppResult<Bill>) = assertIs<AppResult.Success<Bill>>(result).value

    private fun failure(result: AppResult<Bill>) = assertIs<AppResult.Failure>(result).error

    private fun states(bill: Bill) = bill.approvals.associate { it.userId to it.state }

    private fun submitted(): Bill = ok(ApprovalRules.submit(draft(), meera, t0))

    @Test
    fun submittingAsksEveryoneWithAShareExceptThePayerWhoSubmitted() {
        val bill = submitted()
        assertEquals(BillStatus.PENDING, bill.status)
        assertEquals(t0, bill.submittedAt)
        assertEquals(mapOf(aarav to ApprovalState.PENDING, lakshmi to ApprovalState.PENDING), states(bill))
    }

    @Test
    fun aBillOnlyThePayerOwesIsApprovedAtOnce() {
        val rice = Builders.line("a", "Rice", 30_000)
        val solo = draft().copy(
            lines = listOf(rice),
            allocations = split(rice, SplitMode.ONE, meera),
            total = Money(30_000),
        )
        val bill = ok(ApprovalRules.submit(solo, meera, t0))
        assertEquals(BillStatus.APPROVED, bill.status)
        assertEquals(emptyList(), bill.approvals)
    }

    @Test
    fun aMemberWithAZeroShareIsNotAsked() {
        val free = Builders.line("c", "Flyer", 0)
        val withFree = draft().copy(
            lines = draft().lines + free,
            allocations = draft().allocations + split(free, SplitMode.ONE, UserId("u4")),
        )
        assertEquals(
            setOf(aarav, lakshmi),
            ok(ApprovalRules.submit(withFree, meera, t0)).approvals.map {
                it.userId
            }.toSet(),
        )
    }

    @Test
    fun submittingNeedsAnUploaderACompleteSplitAndMatchingTotals() {
        assertEquals(AppError.Forbidden, failure(ApprovalRules.submit(draft(), aarav, t0)))
        assertEquals(
            AppError.Conflict("bill_not_submittable"),
            failure(ApprovalRules.submit(draft().copy(status = BillStatus.APPROVED), meera, t0)),
        )
        val unsplit = draft().copy(allocations = split(rice, SplitMode.ONE, meera))
        assertEquals(
            "lines[1].split",
            assertIs<AppError.Validation>(failure(ApprovalRules.submit(unsplit, meera, t0))).violations.single().field,
        )
        val mismatch = draft().copy(total = Money(1))
        assertEquals(
            "total" to "mismatch",
            assertIs<AppError.Validation>(failure(ApprovalRules.submit(mismatch, meera, t0))).violations.single().let {
                it.field to
                    it.code
            },
        )
    }

    @Test
    fun everyoneApprovingApprovesTheBill() {
        var bill = submitted()
        bill = ok(ApprovalRules.decide(bill, aarav, ApprovalState.APPROVED, null, t0 + 1.hours))
        assertEquals(BillStatus.PENDING, bill.status)
        bill = ok(ApprovalRules.decide(bill, lakshmi, ApprovalState.APPROVED, null, t0 + 2.hours))
        assertEquals(BillStatus.APPROVED, bill.status)
        assertEquals(t0 + 2.hours, bill.approvals.single { it.userId == lakshmi }.decidedAt)
    }

    @Test
    fun aChangeRequestNeedsACommentAndSendsTheBillBackToItsUploader() {
        val bill = submitted()
        assertEquals(
            "comment" to "required",
            assertIs<AppError.Validation>(
                failure(ApprovalRules.decide(bill, aarav, ApprovalState.CHANGES_REQUESTED, " ", t0)),
            ).violations.single().let {
                it.field to
                    it.code
            },
        )
        val asked = ok(ApprovalRules.decide(bill, aarav, ApprovalState.CHANGES_REQUESTED, "Milk was not mine", t0))
        assertEquals(BillStatus.CHANGES_REQUESTED, asked.status)
        assertEquals("Milk was not mine", asked.approvals.single { it.userId == aarav }.comment)
        // Others can no longer decide until the uploader resubmits.
        assertEquals(
            AppError.Conflict("bill_not_pending"),
            failure(ApprovalRules.decide(asked, lakshmi, ApprovalState.APPROVED, null, t0)),
        )
    }

    @Test
    fun onlyAskedMembersMayDecide() {
        assertEquals(
            AppError.Forbidden,
            failure(ApprovalRules.decide(submitted(), meera, ApprovalState.APPROVED, null, t0)),
        )
        assertEquals(
            AppError.Forbidden,
            failure(ApprovalRules.decide(submitted(), UserId("stranger"), ApprovalState.APPROVED, null, t0)),
        )
        assertEquals(
            AppError.Validation(listOf(AppError.FieldViolation("state", "invalid"))),
            failure(ApprovalRules.decide(submitted(), aarav, ApprovalState.PENDING, null, t0)),
        )
    }

    @Test
    fun aMemberMayChangeTheirMindWhileTheBillIsPending() {
        var bill = ok(ApprovalRules.decide(submitted(), aarav, ApprovalState.APPROVED, null, t0))
        bill = ok(ApprovalRules.decide(bill, aarav, ApprovalState.CHANGES_REQUESTED, "Wait", t0 + 1.hours))
        assertEquals(BillStatus.CHANGES_REQUESTED, bill.status)
    }

    @Test
    fun resubmittingKeepsApprovalsAndAsksTheRequesterAgain() {
        var bill = ok(ApprovalRules.decide(submitted(), lakshmi, ApprovalState.APPROVED, null, t0))
        bill = ok(ApprovalRules.decide(bill, aarav, ApprovalState.CHANGES_REQUESTED, "Not mine", t0))
        val again = ok(ApprovalRules.submit(bill, meera, t0 + 30.hours))
        assertEquals(BillStatus.PENDING, again.status)
        assertEquals(mapOf(aarav to ApprovalState.PENDING, lakshmi to ApprovalState.APPROVED), states(again))
        assertEquals(t0 + 30.hours, again.submittedAt)
        assertNull(again.approvals.single { it.userId == aarav }.comment)
    }

    @Test
    fun pendingApprovalsAreGivenAutomaticallyAfter48Hours() {
        val bill = ok(ApprovalRules.decide(submitted(), aarav, ApprovalState.APPROVED, null, t0 + 1.hours))
        assertEquals(bill, ApprovalRules.autoApprove(bill, t0 + 47.hours))
        val done = ApprovalRules.autoApprove(bill, t0 + 48.hours)
        assertEquals(BillStatus.APPROVED, done.status)
        val lakshmiApproval = done.approvals.single { it.userId == lakshmi }
        assertEquals(
            Triple(ApprovalState.APPROVED, true, t0 + 48.hours),
            Triple(lakshmiApproval.state, lakshmiApproval.automatic, lakshmiApproval.decidedAt),
        )
        assertEquals(false, done.approvals.single { it.userId == aarav }.automatic)
    }

    @Test
    fun autoApprovalLeavesChangeRequestsAndDraftsAlone() {
        val asked = ok(ApprovalRules.decide(submitted(), aarav, ApprovalState.CHANGES_REQUESTED, "No", t0))
        assertEquals(asked, ApprovalRules.autoApprove(asked, t0 + 100.hours))
        assertEquals(draft(), ApprovalRules.autoApprove(draft(), t0 + 100.hours))
    }

    @Test
    fun anEditResetsApprovalOnlyForMembersWhoseShareChanged() {
        var approved = ok(ApprovalRules.decide(submitted(), aarav, ApprovalState.APPROVED, null, t0))
        approved = ok(ApprovalRules.decide(approved, lakshmi, ApprovalState.APPROVED, null, t0))
        assertEquals(BillStatus.APPROVED, approved.status)

        // Milk goes to Aarav alone: Aarav's share grows and Lakshmi's drops to nothing.
        val edited = approved.copy(allocations = split(rice, SplitMode.ONE, meera) + split(milk, SplitMode.ONE, aarav))
        val result = ApprovalRules.afterEdit(approved, edited, t0 + 5.hours)
        assertEquals(BillStatus.PENDING, result.status)
        assertEquals(t0 + 5.hours, result.submittedAt)
        assertEquals(mapOf(aarav to ApprovalState.PENDING), states(result))
    }

    @Test
    fun anEditThatChangesNoOnesShareChangesNothing() {
        var approved = ok(ApprovalRules.decide(submitted(), aarav, ApprovalState.APPROVED, null, t0))
        approved = ok(ApprovalRules.decide(approved, lakshmi, ApprovalState.APPROVED, null, t0))
        val renamed = approved.copy(lines = approved.lines.map { it.copy(displayName = "Renamed") })
        assertEquals(renamed, ApprovalRules.afterEdit(approved, renamed, t0 + 5.hours))
    }

    @Test
    fun aNewlyAffectedMemberIsAsked() {
        val approved =
            ok(
                ApprovalRules.decide(
                    ok(ApprovalRules.decide(submitted(), aarav, ApprovalState.APPROVED, null, t0)),
                    lakshmi,
                    ApprovalState.APPROVED,
                    null,
                    t0,
                ),
            )
        val edited = approved.copy(
            allocations =
            split(rice, SplitMode.EQUAL, meera, UserId("u4")) + split(milk, SplitMode.EQUAL, aarav, lakshmi),
        )
        val result = ApprovalRules.afterEdit(approved, edited, t0 + 5.hours)
        assertEquals(ApprovalState.PENDING, states(result)[UserId("u4")])
        assertEquals(ApprovalState.APPROVED, states(result)[aarav])
        assertEquals(BillStatus.PENDING, result.status)
    }

    private fun approvedByBoth(): Bill {
        val first = ok(ApprovalRules.decide(submitted(), aarav, ApprovalState.APPROVED, null, t0))
        return ok(ApprovalRules.decide(first, lakshmi, ApprovalState.APPROVED, null, t0))
    }

    @Test
    fun changingThePayerToSomeoneElseAsksTheUploaderWhoNowOwesToo() {
        val approved = approvedByBoth()
        val result = ApprovalRules.afterEdit(approved, approved.copy(payerId = aarav), t0 + 5.hours)
        assertEquals(
            mapOf(meera to ApprovalState.PENDING, aarav to ApprovalState.APPROVED, lakshmi to ApprovalState.APPROVED),
            states(result),
        )
        assertEquals(BillStatus.PENDING, result.status)
        assertEquals(t0 + 5.hours, result.submittedAt)
    }

    @Test
    fun makingTheUploaderThePayerAgainDropsTheirApproval() {
        val approved = approvedByBoth()
        val reassigned = ApprovalRules.afterEdit(approved, approved.copy(payerId = aarav), t0 + 5.hours)
        val back = ApprovalRules.afterEdit(reassigned, reassigned.copy(payerId = meera), t0 + 6.hours)
        assertEquals(mapOf(aarav to ApprovalState.APPROVED, lakshmi to ApprovalState.APPROVED), states(back))
        assertEquals(BillStatus.APPROVED, back.status)
    }
}
