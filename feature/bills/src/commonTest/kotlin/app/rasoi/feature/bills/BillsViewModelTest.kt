package app.rasoi.feature.bills

import app.cash.turbine.test
import app.rasoi.core.domain.bills.CreateManualBillUseCase
import app.rasoi.core.domain.bills.ListBillsUseCase
import app.rasoi.core.model.AppError
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Platform
import app.rasoi.core.model.Role
import app.rasoi.core.model.UserId
import app.rasoi.core.testing.Builders
import app.rasoi.core.testing.FakeBillRepository
import app.rasoi.core.testing.FakeHouseholdRepository
import app.rasoi.feature.bills.list.BillsEffect
import app.rasoi.feature.bills.list.BillsUiEvent
import app.rasoi.feature.bills.list.BillsUiState
import app.rasoi.feature.bills.list.BillsViewModel
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BillsViewModelTest : ViewModelTest() {
    private val households = FakeHouseholdRepository(
        details = Builders.details(
            members = listOf(Builders.member("u1", "Meera"), Builders.member("u2", "Aarav", Role.MEMBER)),
        ),
    )
    private val repository = FakeBillRepository().apply {
        bills[BillId("old")] = Builders.bill("old")
        bills[BillId("new")] =
            Builders.bill("new", status = BillStatus.PENDING).copy(invoiceDate = LocalDate(2026, 10, 9))
    }

    private fun viewModel(role: Role = Role.MEMBER) = BillsViewModel(
        HouseholdId("h1"),
        role,
        households,
        ListBillsUseCase(repository),
        CreateManualBillUseCase(repository),
    )

    private fun BillsViewModel.content() = assertIs<BillsUiState.Content>(state.value)

    @Test
    fun listsBillsNewestFirstWithMemberNames() {
        val content = viewModel().content()
        assertEquals(listOf("new", "old"), content.bills.map { it.id.value })
        assertEquals("Meera", content.names[UserId("u1")])
        assertTrue(content.canEdit)
        assertNull(content.form)
    }

    @Test
    fun anEmptyHouseholdShowsNoBills() {
        repository.bills.clear()
        assertEquals(emptyList(), viewModel().content().bills)
    }

    @Test
    fun cooksNeverSeeMoney() {
        val state = viewModel(Role.COOK).state.value
        assertEquals(BillsUiState.Error(BillsError.FORBIDDEN), state)
    }

    @Test
    fun aFailedLoadCanBeRetried() {
        repository.failure = AppError.Offline
        val viewModel = viewModel()
        assertEquals(BillsUiState.Error(BillsError.OFFLINE), viewModel.state.value)
        repository.failure = null
        viewModel.onEvent(BillsUiEvent.Retry)
        assertEquals(2, viewModel.content().bills.size)
    }

    @Test
    fun refreshingPicksUpChangesWithoutBlankingTheList() {
        val viewModel = viewModel()
        repository.bills[BillId("third")] = Builders.bill("third")
        viewModel.onEvent(BillsUiEvent.Refresh)
        assertEquals(3, viewModel.content().bills.size)
        repository.failure = AppError.Offline
        viewModel.onEvent(BillsUiEvent.Refresh)
        assertEquals(3, viewModel.content().bills.size)
    }

    @Test
    fun openingABillAndImportingAreOneOffEffects() = runTest {
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.onEvent(BillsUiEvent.OpenBill(BillId("new")))
            assertEquals(BillsEffect.OpenBill(BillId("new")), awaitItem())
            viewModel.onEvent(BillsUiEvent.ImportInvoice)
            assertEquals(BillsEffect.OpenImport, awaitItem())
        }
    }

    @Test
    fun theNewBillFormValidatesBeforeCallingTheServer() {
        val viewModel = viewModel()
        viewModel.onEvent(BillsUiEvent.OpenNew)
        viewModel.onEvent(BillsUiEvent.SaveNew)
        val form = assertIs<BillsUiState.Content>(viewModel.state.value).form
        assertEquals(FieldError.REQUIRED, form?.titleError)
        assertEquals(FieldError.INVALID, form?.amountError)
        viewModel.onEvent(BillsUiEvent.TitleChanged("Gas"))
        viewModel.onEvent(BillsUiEvent.AmountChanged("0"))
        viewModel.onEvent(BillsUiEvent.SaveNew)
        assertEquals(FieldError.INVALID, assertIs<BillsUiState.Content>(viewModel.state.value).form?.amountError)
        assertEquals(2, repository.bills.size)
    }

    @Test
    fun savingANewBillOpensItForSplitting() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(BillsUiEvent.OpenNew)
        viewModel.onEvent(BillsUiEvent.TitleChanged("Gas cylinder"))
        viewModel.onEvent(BillsUiEvent.AmountChanged("950.00"))
        viewModel.effects.test {
            viewModel.onEvent(BillsUiEvent.SaveNew)
            val opened = assertIs<BillsEffect.OpenBill>(awaitItem())
            val bill = repository.bills.getValue(opened.billId)
            assertEquals(Platform.MANUAL, bill.platform)
            assertEquals("Gas cylinder", bill.title)
            assertEquals(95_000L, bill.total.paise)
        }
        assertNull(viewModel.content().form)
    }

    @Test
    fun aFailedSaveKeepsTheFormAndShowsWhy() {
        val viewModel = viewModel()
        viewModel.onEvent(BillsUiEvent.OpenNew)
        viewModel.onEvent(BillsUiEvent.TitleChanged("Gas"))
        viewModel.onEvent(BillsUiEvent.AmountChanged("950"))
        repository.failure = AppError.Offline
        viewModel.onEvent(BillsUiEvent.SaveNew)
        val form = assertIs<BillsUiState.Content>(viewModel.state.value).form
        assertEquals(BillsError.OFFLINE, form?.error)
        assertEquals("Gas", form?.title)
    }

    @Test
    fun pullingToRefreshReloadsAndAFailedPullKeepsWhatIsShown() {
        val viewModel = viewModel()
        val before = viewModel.content()
        viewModel.onEvent(BillsUiEvent.PullToRefresh)
        assertEquals(before, viewModel.content())
        repository.failure = AppError.Offline
        viewModel.onEvent(BillsUiEvent.PullToRefresh)
        assertEquals(before, viewModel.content())
        assertFalse(viewModel.content().isRefreshing)
    }
}
