package app.rasoi.feature.bills

import app.cash.turbine.test
import app.rasoi.core.domain.bills.DecideBillUseCase
import app.rasoi.core.domain.bills.GetBillUseCase
import app.rasoi.core.domain.bills.SubmitBillUseCase
import app.rasoi.core.domain.bills.UpdateBillUseCase
import app.rasoi.core.model.AppError
import app.rasoi.core.model.Approval
import app.rasoi.core.model.ApprovalState
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.ChargeDistribution
import app.rasoi.core.model.ChargeId
import app.rasoi.core.model.Role
import app.rasoi.core.model.SplitMode
import app.rasoi.core.model.UserId
import app.rasoi.core.testing.Builders
import app.rasoi.core.testing.FakeBillRepository
import app.rasoi.core.testing.FakeHouseholdRepository
import app.rasoi.core.testing.FixedClock
import app.rasoi.feature.bills.detail.BillDetailEffect
import app.rasoi.feature.bills.detail.BillDetailUiEvent
import app.rasoi.feature.bills.detail.BillDetailUiState
import app.rasoi.feature.bills.detail.BillDetailViewModel
import app.rasoi.feature.bills.detail.BillSheet
import app.rasoi.feature.bills.detail.SplitError
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BillDetailViewModelTest : ViewModelTest() {
    private val meera = UserId("u1")
    private val aarav = UserId("u2")
    private val lakshmi = UserId("u3")
    private val rice = Builders.line("a", "Rice", 30_000, units = 3)
    private val milk = Builders.line("b", "Milk", 10_000)
    private val households = FakeHouseholdRepository(
        details = Builders.details(
            members = listOf(
                Builders.member("u1", "Meera"),
                Builders.member("u2", "Aarav", Role.MEMBER),
                Builders.member("u3", "Lakshmi", Role.COOK),
            ),
        ),
    )
    private val repository = FakeBillRepository(me = meera)

    private fun bill(
        status: BillStatus = BillStatus.DRAFT,
        split: Boolean = true,
        approvals: List<Approval> = emptyList(),
    ): Bill = Builders.bill(
        lines = listOf(rice, milk),
        charges = listOf(Builders.charge("c1", amount = 600)),
        status = status,
        approvals = approvals,
        allocations = if (split) {
            Builders.split(rice, SplitMode.EQUAL, "u1", "u2") + Builders.split(milk, SplitMode.ONE, "u2")
        } else {
            emptyList()
        },
    ).also { repository.bills[it.id] = it }

    private fun viewModel(me: UserId = meera, role: Role = Role.OWNER): BillDetailViewModel {
        repository.me = me
        val clock = FixedClock()
        return BillDetailViewModel(
            BillId("b1"),
            me,
            role,
            households,
            GetBillUseCase(repository),
            UpdateBillUseCase(repository),
            SubmitBillUseCase(repository, clock),
            DecideBillUseCase(repository, clock),
        )
    }

    private fun BillDetailViewModel.content() = assertIs<BillDetailUiState.Content>(state.value)

    private fun BillDetailViewModel.sheet() = content().sheet

    @Test
    fun loadsTheBillWithNamesSharesAndWhatTheViewerMayDo() {
        bill()
        val content = viewModel().content()
        assertEquals("Aarav", content.names[aarav])
        assertEquals(listOf(meera, aarav), content.shares.map { it.userId })
        assertEquals(40_600L, content.shares.sumOf { it.total.paise })
        assertEquals(listOf(meera, aarav), content.participants.map { it.userId })
        assertTrue(content.canEdit && content.canSubmit && content.canEditItems)
        assertFalse(content.canApprove)
    }

    @Test
    fun aMissingBillIsAnError() {
        assertEquals(BillDetailUiState.Error(BillsError.NOT_FOUND), viewModel().state.value)
    }

    @Test
    fun membersOtherThanTheUploaderCannotEditASubmittedBill() {
        bill(BillStatus.PENDING, approvals = listOf(Approval(aarav, ApprovalState.PENDING)))
        val content = viewModel(aarav, Role.MEMBER).content()
        assertFalse(content.canEdit || content.canSubmit || content.canEditItems)
        assertTrue(content.canApprove && content.canRequestChanges)
        assertTrue(viewModel(meera, Role.OWNER).content().canEdit)
    }

    @Test
    fun theSplitSheetStartsWithEveryoneWhoCanOweAndOffersNoCooks() {
        bill(split = false)
        val viewModel = viewModel()
        viewModel.onEvent(BillDetailUiEvent.OpenSplit(BillLineId("a")))
        val sheet = assertIs<BillSheet.Split>(viewModel.sheet())
        assertEquals(SplitMode.EQUAL, sheet.mode)
        assertEquals(listOf(meera, aarav), sheet.units.keys.toList())
        assertFalse(lakshmi in sheet.units)
    }

    @Test
    fun theSplitSheetShowsTheCurrentSplit() {
        bill()
        val viewModel = viewModel()
        viewModel.onEvent(BillDetailUiEvent.OpenSplit(BillLineId("b")))
        val sheet = assertIs<BillSheet.Split>(viewModel.sheet())
        assertEquals(SplitMode.ONE, sheet.mode)
        assertEquals(listOf(aarav), sheet.units.keys.toList())
    }

    @Test
    fun modeAndMemberChoicesEditTheSheet() {
        bill(split = false)
        val viewModel = viewModel()
        viewModel.onEvent(BillDetailUiEvent.OpenSplit(BillLineId("a")))
        viewModel.onEvent(BillDetailUiEvent.Split.ModeSelected(SplitMode.ONE))
        assertEquals(listOf(meera), assertIs<BillSheet.Split>(viewModel.sheet()).units.keys.toList())
        viewModel.onEvent(BillDetailUiEvent.Split.MemberToggled(aarav))
        assertEquals(listOf(aarav), assertIs<BillSheet.Split>(viewModel.sheet()).units.keys.toList())
        viewModel.onEvent(BillDetailUiEvent.Split.ModeSelected(SplitMode.EQUAL))
        viewModel.onEvent(BillDetailUiEvent.Split.MemberToggled(meera))
        assertEquals(listOf(aarav, meera), assertIs<BillSheet.Split>(viewModel.sheet()).units.keys.toList())
        viewModel.onEvent(BillDetailUiEvent.Split.MemberToggled(aarav))
        viewModel.onEvent(BillDetailUiEvent.Split.MemberToggled(meera))
        assertEquals(emptyList(), assertIs<BillSheet.Split>(viewModel.sheet()).units.keys.toList())
    }

    @Test
    fun savingASplitUpdatesTheBillAndClosesTheSheet() {
        bill(split = false)
        val viewModel = viewModel()
        viewModel.onEvent(BillDetailUiEvent.OpenSplit(BillLineId("a")))
        viewModel.onEvent(BillDetailUiEvent.Split.Save)
        val content = viewModel.content()
        assertNull(content.sheet)
        assertEquals(listOf(15_000L, 15_000L), content.bill.allocations.map { it.amount.paise })
        assertEquals(1, repository.updates.size)
    }

    @Test
    fun anEmptyOrWrongSplitIsRefusedLocallyWithoutCallingTheServer() {
        bill(split = false)
        val viewModel = viewModel()
        viewModel.onEvent(BillDetailUiEvent.OpenSplit(BillLineId("a")))
        viewModel.onEvent(BillDetailUiEvent.Split.MemberToggled(meera))
        viewModel.onEvent(BillDetailUiEvent.Split.MemberToggled(aarav))
        viewModel.onEvent(BillDetailUiEvent.Split.Save)
        assertEquals(SplitError.NO_MEMBERS, assertIs<BillSheet.Split>(viewModel.sheet()).error)

        viewModel.onEvent(BillDetailUiEvent.Split.ModeSelected(SplitMode.QUANTITY))
        viewModel.onEvent(BillDetailUiEvent.Split.MemberToggled(meera))
        viewModel.onEvent(BillDetailUiEvent.Split.Save)
        assertEquals(SplitError.UNITS, assertIs<BillSheet.Split>(viewModel.sheet()).error)
        assertTrue(repository.updates.isEmpty())
    }

    @Test
    fun quantitySplitsStepEachPersonsUnitsWithinTheLine() {
        bill(split = false)
        val viewModel = viewModel()
        viewModel.onEvent(BillDetailUiEvent.OpenSplit(BillLineId("a")))
        viewModel.onEvent(BillDetailUiEvent.Split.ModeSelected(SplitMode.QUANTITY))
        viewModel.onEvent(BillDetailUiEvent.Split.UnitsStepped(meera, 5))
        assertEquals(3, assertIs<BillSheet.Split>(viewModel.sheet()).units.getValue(meera))
        viewModel.onEvent(BillDetailUiEvent.Split.UnitsStepped(meera, -5))
        assertEquals(1, assertIs<BillSheet.Split>(viewModel.sheet()).units.getValue(meera))
        viewModel.onEvent(BillDetailUiEvent.Split.UnitsStepped(meera, 1))
        viewModel.onEvent(BillDetailUiEvent.Split.Save)
        val allocations = viewModel.content().bill.allocations.filter { it.lineId == BillLineId("a") }
        assertEquals(listOf(20_000L, 10_000L), allocations.map { it.amount.paise })
    }

    @Test
    fun theSheetCanBeDismissed() {
        bill()
        val viewModel = viewModel()
        viewModel.onEvent(BillDetailUiEvent.OpenPayer)
        assertEquals(BillSheet.Payer, viewModel.sheet())
        viewModel.onEvent(BillDetailUiEvent.OpenCharge(ChargeId("c1")))
        viewModel.onEvent(BillDetailUiEvent.DismissSheet)
        assertNull(viewModel.sheet())
    }

    @Test
    fun changingThePayerSavesAndCloses() {
        bill()
        val viewModel = viewModel()
        viewModel.onEvent(BillDetailUiEvent.OpenPayer)
        viewModel.onEvent(BillDetailUiEvent.Change.PayerSelected(aarav))
        assertEquals(aarav, viewModel.content().bill.payerId)
        assertNull(viewModel.sheet())
    }

    @Test
    fun aChargeCanBeSharedEqually() {
        bill()
        val viewModel = viewModel()
        viewModel.onEvent(BillDetailUiEvent.OpenCharge(ChargeId("c1")))
        viewModel.onEvent(BillDetailUiEvent.Change.DistributionSelected(ChargeDistribution.EQUAL))
        assertEquals(ChargeDistribution.EQUAL, viewModel.content().bill.charges.single().distribution)
    }

    @Test
    fun submittingAnUnsplitBillIsRefusedAndSplitBillsAreSent() {
        bill(split = false)
        val viewModel = viewModel()
        viewModel.onEvent(BillDetailUiEvent.Submit)
        assertEquals(BillsError.NOT_SPLIT, viewModel.content().error)
        assertTrue(repository.submitted.isEmpty())

        bill()
        val ready = viewModel()
        ready.onEvent(BillDetailUiEvent.Submit)
        assertEquals(BillStatus.PENDING, ready.content().bill.status)
        assertEquals(listOf(aarav), ready.content().bill.approvals.map { it.userId })
        assertNull(ready.content().error)
    }

    @Test
    fun anApproverCanApproveOrAskForChanges() {
        bill(BillStatus.PENDING, approvals = listOf(Approval(aarav, ApprovalState.PENDING)))
        val approver = viewModel(aarav, Role.MEMBER)
        approver.onEvent(BillDetailUiEvent.Approve)
        assertEquals(BillStatus.APPROVED, approver.content().bill.status)

        bill(BillStatus.PENDING, approvals = listOf(Approval(aarav, ApprovalState.PENDING)))
        val critic = viewModel(aarav, Role.MEMBER)
        val decisionsBefore = repository.decisions.size
        critic.onEvent(BillDetailUiEvent.OpenRequestChanges)
        critic.onEvent(BillDetailUiEvent.Change.SendChangeRequest)
        assertEquals(FieldError.REQUIRED, assertIs<BillSheet.RequestChanges>(critic.sheet()).commentError)
        assertEquals(decisionsBefore, repository.decisions.size)
        critic.onEvent(BillDetailUiEvent.Change.CommentChanged("Milk was mine"))
        critic.onEvent(BillDetailUiEvent.Change.SendChangeRequest)
        assertEquals(BillStatus.CHANGES_REQUESTED, critic.content().bill.status)
        assertNull(critic.sheet())
    }

    @Test
    fun aFailedSaveKeepsTheSheetOpenWithTheError() {
        bill(split = false)
        val viewModel = viewModel()
        viewModel.onEvent(BillDetailUiEvent.OpenSplit(BillLineId("b")))
        repository.failure = AppError.Offline
        viewModel.onEvent(BillDetailUiEvent.Split.Save)
        val content = viewModel.content()
        assertIs<BillSheet.Split>(content.sheet)
        assertEquals(BillsError.OFFLINE, content.error)
        assertFalse(content.isBusy)
    }

    @Test
    fun aConflictingChangeReloadsTheBill() {
        bill(split = false)
        val viewModel = viewModel()
        viewModel.onEvent(BillDetailUiEvent.OpenSplit(BillLineId("b")))
        repository.bills[BillId("b1")] = repository.bills.getValue(BillId("b1")).copy(payerId = aarav)
        repository.failure = AppError.Conflict("bill_changed")
        viewModel.onEvent(BillDetailUiEvent.Split.Save)
        assertEquals(BillsError.CHANGED, viewModel.content().error)
        repository.failure = null
        viewModel.onEvent(BillDetailUiEvent.Retry)
        assertEquals(aarav, viewModel.content().bill.payerId)
    }

    @Test
    fun editingTheItemsIsAnEffectForTheNavigator() = runTest {
        bill()
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.onEvent(BillDetailUiEvent.EditItems)
            assertEquals(BillDetailEffect.OpenItems(BillId("b1")), awaitItem())
        }
    }

    @Test
    fun pullingToRefreshReloadsAndAFailedPullKeepsWhatIsShown() {
        bill()
        val viewModel = viewModel()
        val before = viewModel.content()
        viewModel.onEvent(BillDetailUiEvent.PullToRefresh)
        assertEquals(before, viewModel.content())
        repository.failure = AppError.Offline
        viewModel.onEvent(BillDetailUiEvent.PullToRefresh)
        assertEquals(before, viewModel.content())
        assertFalse(viewModel.content().isRefreshing)
    }
}
