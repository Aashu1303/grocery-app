package app.rasoi.core.domain.bills

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Approval
import app.rasoi.core.model.ApprovalState
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Money
import app.rasoi.core.model.Platform
import app.rasoi.core.model.Role
import app.rasoi.core.model.SplitMode
import app.rasoi.core.model.UserId
import app.rasoi.core.testing.Builders
import app.rasoi.core.testing.FakeBillRepository
import app.rasoi.core.testing.FixedClock
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BillWorkflowUseCasesTest {
    private val clock = FixedClock()
    private val repository = FakeBillRepository(clock = clock)
    private val householdId = HouseholdId("h1")
    private val meera = UserId("u1")
    private val aarav = UserId("u2")

    private fun invalid(field: String, code: String) =
        AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation(field, code))))

    private fun splitBill(status: BillStatus = BillStatus.DRAFT, approvals: List<Approval> = emptyList()): Bill {
        val rice = Builders.line("a", "Rice", 3_000)
        val milk = Builders.line("b", "Milk", 1_000)
        return Builders.bill(
            lines = listOf(rice, milk),
            status = status,
            approvals = approvals,
            allocations = Builders.split(rice, SplitMode.EQUAL, "u1", "u2") + Builders.split(milk, SplitMode.ONE, "u2"),
        ).also { repository.bills[it.id] = it }
    }

    @Test
    fun cooksCannotListBillsAndOthersGetThemNewestFirst() = runTest {
        repository.bills[Builders.bill("old").id] = Builders.bill("old")
        repository.bills[Builders.bill("new").id] = Builders.bill("new").copy(invoiceDate = LocalDate(2026, 10, 9))
        val list = ListBillsUseCase(repository)
        assertEquals(AppResult.Failure(AppError.Forbidden), list(householdId, Role.COOK))
        val bills = assertIs<AppResult.Success<*>>(list(householdId, Role.MEMBER)).value as List<*>
        assertEquals(2, bills.size)
    }

    @Test
    fun aManualBillNeedsATitleAndLinesAndTheRightRole() = runTest {
        val create = CreateManualBillUseCase(repository)
        val line = LineEdit(null, "Cylinder", 1, Money(95_000))
        val draft = ManualBillDraft("Gas", null, meera, listOf(line), emptyList())
        assertEquals(AppResult.Failure(AppError.Forbidden), create(householdId, Role.COOK, draft))
        assertEquals(invalid("title", "required"), create(householdId, Role.MEMBER, draft.copy(title = " ")))
        assertEquals(invalid("lines", "required"), create(householdId, Role.MEMBER, draft.copy(lines = emptyList())))
        assertTrue(repository.bills.isEmpty())

        val created = assertIs<AppResult.Success<Bill>>(create(householdId, Role.MEMBER, draft)).value
        assertEquals(Platform.MANUAL, created.platform)
        assertEquals(Money(95_000), created.total)
        assertEquals(LocalDate(2026, 10, 11), created.invoiceDate)
        val withCharge = draft.copy(charges = listOf(ChargeEdit(null, ChargeKind.DELIVERY, "Delivery", Money(2_000))))
        assertEquals(
            Money(97_000),
            (create(householdId, Role.MEMBER, withCharge) as AppResult.Success).value.total,
        )
    }

    @Test
    fun submitRefusesABillThatIsNotFullySplitWithoutCallingTheServer() = runTest {
        val unsplit = Builders.bill("b2").also { repository.bills[it.id] = it }
        val result = SubmitBillUseCase(repository, clock)(unsplit, meera)
        assertEquals(invalid("lines[0].split", "missing"), result)
        assertTrue(repository.submitted.isEmpty())
    }

    @Test
    fun submitSendsASplitBillAndOnlyItsUploaderMay() = runTest {
        val bill = splitBill()
        val submit = SubmitBillUseCase(repository, clock)
        assertEquals(AppResult.Failure(AppError.Forbidden), submit(bill, aarav))
        val sent = assertIs<AppResult.Success<Bill>>(submit(bill, meera)).value
        assertEquals(BillStatus.PENDING, sent.status)
        assertEquals(listOf(aarav), sent.approvals.map { it.userId })
        assertEquals(listOf(bill.id), repository.submitted)
    }

    @Test
    fun decideNeedsACommentToRequestChangesAndTrimsIt() = runTest {
        repository.me = aarav
        val pending = splitBill(BillStatus.PENDING, listOf(Approval(aarav, ApprovalState.PENDING)))
        val decide = DecideBillUseCase(repository, clock)
        assertEquals(
            invalid("comment", "required"),
            decide(pending, aarav, ApprovalState.CHANGES_REQUESTED, "  "),
        )
        assertTrue(repository.decisions.isEmpty())
        val result = decide(pending, aarav, ApprovalState.CHANGES_REQUESTED, "  Milk was mine  ")
        assertEquals(BillStatus.CHANGES_REQUESTED, (result as AppResult.Success).value.status)
        val expected = listOf<Triple<BillId, ApprovalState, String?>>(
            Triple(pending.id, ApprovalState.CHANGES_REQUESTED, "Milk was mine"),
        )
        assertEquals(expected, repository.decisions)
    }

    @Test
    fun onlyMembersWhoOweCanApprove() = runTest {
        repository.me = UserId("u9")
        val pending = splitBill(BillStatus.PENDING, listOf(Approval(aarav, ApprovalState.PENDING)))
        val result = DecideBillUseCase(repository, clock)(pending, UserId("u9"), ApprovalState.APPROVED, null)
        assertEquals(AppResult.Failure(AppError.Forbidden), result)
    }

    @Test
    fun editingASubmittedBillAsksTheMembersWhoseShareMovedAgain() = runTest {
        val approved = splitBill(
            BillStatus.APPROVED,
            listOf(Approval(aarav, ApprovalState.APPROVED, decidedAt = clock.now())),
        )
        val edit = BillEdit(payerId = aarav)
        val updated = (UpdateBillUseCase(repository)(approved.id, edit) as AppResult.Success).value
        assertEquals(BillStatus.PENDING, updated.status)
        assertEquals(ApprovalState.PENDING, updated.approvals.first { it.userId == meera }.state)
    }
}
