package app.rasoi.feature.ledger

import app.cash.turbine.test
import app.rasoi.core.domain.ledger.CloseMonthUseCase
import app.rasoi.core.domain.ledger.GetLedgerUseCase
import app.rasoi.core.domain.ledger.MarkPaidUseCase
import app.rasoi.core.domain.ledger.UpdateUpiUseCase
import app.rasoi.core.model.AppError
import app.rasoi.core.model.Balance
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Ledger
import app.rasoi.core.model.LedgerTransfer
import app.rasoi.core.model.Money
import app.rasoi.core.model.PeriodMonth
import app.rasoi.core.model.PeriodStatus
import app.rasoi.core.model.Role
import app.rasoi.core.model.SettlementId
import app.rasoi.core.model.SettlementStatus
import app.rasoi.core.model.Transfer
import app.rasoi.core.model.UserId
import app.rasoi.core.testing.Builders
import app.rasoi.core.testing.FakeHouseholdRepository
import app.rasoi.core.testing.FakeLedgerRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LedgerViewModelTest : ViewModelTest() {
    private val meera = UserId("u1")
    private val aarav = UserId("u2")
    private val october = PeriodMonth(2026, 10)
    private val households = FakeHouseholdRepository(
        details = Builders.details(
            members = listOf(Builders.member("u1", "Meera"), Builders.member("u2", "Aarav", Role.MEMBER)),
        ),
    )
    private val open = Ledger(
        october,
        PeriodStatus.OPEN,
        listOf(Balance(meera, Money(15_000)), Balance(aarav, Money(-15_000))),
        listOf(LedgerTransfer(Transfer(aarav, meera, Money(15_000)), payeeUpiVpa = "meera@okbank")),
        pendingBills = 1,
        months = listOf(october),
    )
    private val repository = FakeLedgerRepository(open)

    private fun viewModel(me: UserId = meera, role: Role = Role.OWNER) = LedgerViewModel(
        HouseholdId("h1"),
        me,
        role,
        households,
        repository,
        GetLedgerUseCase(repository),
        CloseMonthUseCase(repository),
        MarkPaidUseCase(repository),
        UpdateUpiUseCase(repository),
    )

    private fun LedgerViewModel.content() = assertIs<LedgerUiState.Content>(state.value)

    @Test
    fun loadsTheOpenLedgerWithNamesAndMyUpi() {
        repository.upiVpa = "meera@okbank"
        val content = viewModel().content()
        assertEquals(PeriodStatus.OPEN, content.ledger.status)
        assertEquals("Aarav", content.names[aarav])
        assertEquals("meera@okbank", content.myUpi)
        assertTrue(content.canClose)
        assertEquals(1, content.ledger.pendingBills)
    }

    @Test
    fun cooksNeverSeeMoneyAndFailuresCanBeRetried() {
        assertEquals(LedgerUiState.Error(LedgerError.FORBIDDEN), viewModel(role = Role.COOK).state.value)
        repository.failure = AppError.Offline
        val viewModel = viewModel()
        assertEquals(LedgerUiState.Error(LedgerError.OFFLINE), viewModel.state.value)
        repository.failure = null
        viewModel.onEvent(LedgerUiEvent.Retry)
        assertIs<LedgerUiState.Content>(viewModel.state.value)
    }

    @Test
    fun onlyOwnersCanAskToCloseAndSuggestionsCannotBePaid() {
        val member = viewModel(aarav, Role.MEMBER)
        assertEquals(false, member.content().canClose)
        member.onEvent(LedgerUiEvent.AskClose)
        assertNull(member.content().sheet)
        val item = member.content().ledger.transfers.single()
        assertEquals(false, member.content().canPayByUpi(item))
        assertEquals(false, member.content().canMarkPaid(item))
    }

    @Test
    fun closingTheMonthShowsTheStoredTransfers() {
        val viewModel = viewModel()
        viewModel.onEvent(LedgerUiEvent.AskClose)
        assertEquals(LedgerSheet.ConfirmClose, viewModel.content().sheet)
        viewModel.onEvent(LedgerUiEvent.ConfirmClose)
        val content = viewModel.content()
        assertEquals(PeriodStatus.LOCKED, content.ledger.status)
        assertEquals(SettlementStatus.PENDING, content.ledger.transfers.single().status)
        assertNull(content.sheet)
        assertEquals(false, content.canClose)
    }

    @Test
    fun aMonthThatIsAlreadyClosedShowsTheConflictAndReloads() {
        val viewModel = viewModel()
        viewModel.onEvent(LedgerUiEvent.AskClose)
        repository.open = open.copy(month = PeriodMonth(2026, 11))
        viewModel.onEvent(LedgerUiEvent.ConfirmClose)
        assertEquals(LedgerError.CHANGED, viewModel.content().error)
        assertEquals(PeriodMonth(2026, 11), viewModel.content().ledger.month)
    }

    @Test
    fun thePayerOpensAUpiLinkForAStoredTransfer() = runTest {
        val payer = viewModel(aarav, Role.MEMBER)
        repository.closeMonth(HouseholdId("h1"), october)
        payer.onEvent(LedgerUiEvent.Retry)
        val settlement = payer.content().ledger.transfers.single().settlementId ?: error("not stored")
        assertTrue(payer.content().canPayByUpi(payer.content().ledger.transfers.single()))
        payer.effects.test {
            payer.onEvent(LedgerUiEvent.PayWithUpi(settlement))
            val link = assertIs<LedgerEffect.OpenUpi>(awaitItem()).link
            assertEquals("upi://pay?pa=meera@okbank&pn=Meera&am=150.00&cu=INR&tn=Rasoi%202026-10", link)
        }
        payer.onEvent(LedgerUiEvent.Upi.Unavailable)
        assertEquals(LedgerError.NO_UPI_APP, payer.content().error)
    }

    @Test
    fun eitherSideCanMarkAStoredTransferPaid() {
        repository.open = open
        repository.closed.clear()
        runTest { repository.closeMonth(HouseholdId("h1"), october) }
        val viewModel = viewModel(meera, Role.MEMBER)
        viewModel.onEvent(LedgerUiEvent.SelectMonth(october))
        val settlement = viewModel.content().ledger.transfers.single().settlementId ?: error("not stored")
        viewModel.onEvent(LedgerUiEvent.MarkPaid(settlement))
        assertEquals(listOf<SettlementId>(settlement), repository.paid)
        assertEquals(SettlementStatus.PAID, viewModel.content().ledger.transfers.single().status)
    }

    @Test
    fun theUpiAddressIsValidatedAndSaved() {
        val viewModel = viewModel()
        viewModel.onEvent(LedgerUiEvent.Upi.Edit)
        viewModel.onEvent(LedgerUiEvent.Upi.Changed("not a vpa"))
        viewModel.onEvent(LedgerUiEvent.Upi.Save)
        assertEquals(true, (viewModel.content().sheet as LedgerSheet.Upi).invalid)
        assertNull(repository.upiVpa)
        viewModel.onEvent(LedgerUiEvent.Upi.Changed("meera@okbank"))
        viewModel.onEvent(LedgerUiEvent.Upi.Save)
        assertEquals("meera@okbank", viewModel.content().myUpi)
        assertNull(viewModel.content().sheet)
    }

    @Test
    fun pullingToRefreshReloadsAndAFailedPullKeepsWhatIsShown() {
        val viewModel = viewModel()
        val before = viewModel.content()
        viewModel.onEvent(LedgerUiEvent.PullToRefresh)
        assertEquals(before, viewModel.content())
        repository.failure = AppError.Offline
        viewModel.onEvent(LedgerUiEvent.PullToRefresh)
        assertEquals(before, viewModel.content())
        assertFalse(viewModel.content().isRefreshing)
    }
}
