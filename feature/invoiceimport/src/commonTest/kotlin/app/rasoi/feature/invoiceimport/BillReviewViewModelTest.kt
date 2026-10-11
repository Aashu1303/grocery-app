package app.rasoi.feature.invoiceimport

import app.rasoi.core.domain.bills.BillRules
import app.rasoi.core.domain.bills.GetBillUseCase
import app.rasoi.core.domain.bills.UpdateBillUseCase
import app.rasoi.core.model.AppError
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.BillWarning
import app.rasoi.core.model.ImportConfidence
import app.rasoi.core.model.Money
import app.rasoi.core.testing.Builders
import app.rasoi.core.testing.FakeBillRepository
import app.rasoi.feature.invoiceimport.review.BillReviewUiEvent
import app.rasoi.feature.invoiceimport.review.BillReviewUiState
import app.rasoi.feature.invoiceimport.review.BillReviewViewModel
import app.rasoi.feature.invoiceimport.review.LineFieldError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BillReviewViewModelTest : ViewModelTest() {
    private val onion = Builders.line(id = "l1", name = "Onion", amount = 4_000)
    private val tea = Builders.line(id = "l2", name = "Tea", amount = 9_000, units = 2)
    private val repository = FakeBillRepository().apply {
        val bill = Builders.bill(id = "b1", lines = listOf(onion, tea), total = Money(13_100))
        bills[bill.id] = bill
    }

    private fun viewModel() =
        BillReviewViewModel(BillId("b1"), GetBillUseCase(repository), UpdateBillUseCase(repository))

    private fun BillReviewViewModel.content() = assertIs<BillReviewUiState.Content>(state.value)

    @Test
    fun loadsTheDraftWithItsMismatchWarning() {
        val content = viewModel().content()
        assertEquals(
            listOf<BillWarning>(BillWarning.TotalMismatch(computed = Money(13_000), printed = Money(13_100))),
            content.warnings,
        )
        assertNull(content.edit)
    }

    @Test
    fun aBillReadByTheFallbackIsFlagged() {
        repository.bills[BillId("b1")] = Builders.bill(id = "b1", confidence = ImportConfidence.LOW)
        assertEquals(listOf<BillWarning>(BillWarning.ParsedByFallback), viewModel().content().warnings)
    }

    @Test
    fun loadFailureShowsErrorAndRetryRecovers() {
        repository.failure = AppError.Offline
        val viewModel = viewModel()
        assertEquals(BillReviewUiState.Error(BillError.OFFLINE), viewModel.state.value)
        repository.failure = null
        viewModel.onEvent(BillReviewUiEvent.Retry)
        viewModel.content()
    }

    @Test
    fun aMissingBillIsNotFound() {
        val viewModel = BillReviewViewModel(BillId("nope"), GetBillUseCase(repository), UpdateBillUseCase(repository))
        assertEquals(BillReviewUiState.Error(BillError.NOT_FOUND), viewModel.state.value)
    }

    @Test
    fun editingALinePrefillsTheSheet() {
        val viewModel = viewModel()
        viewModel.onEvent(BillReviewUiEvent.EditLine(BillLineId("l2")))
        val edit = assertNotNull(viewModel.content().edit)
        assertEquals(Triple("Tea", 2, "90.00"), Triple(edit.name, edit.units, edit.amountText))
        viewModel.onEvent(BillReviewUiEvent.EditLine(BillLineId("missing")))
        assertEquals(BillLineId("l2"), viewModel.content().edit?.lineId)
    }

    @Test
    fun savingAnEditedLineUpdatesTheBillAndClearsTheWarning() {
        val viewModel = viewModel()
        viewModel.onEvent(BillReviewUiEvent.EditLine(BillLineId("l1")))
        viewModel.onEvent(BillReviewUiEvent.NameChanged("Red onion"))
        viewModel.onEvent(BillReviewUiEvent.UnitsStepped(1))
        viewModel.onEvent(BillReviewUiEvent.AmountChanged("41.00"))
        viewModel.onEvent(BillReviewUiEvent.SaveLine)

        val content = viewModel.content()
        assertNull(content.edit)
        val saved = content.bill.lines.first()
        assertEquals(Triple("Red onion", 2, Money(4_100)), Triple(saved.displayName, saved.units, saved.amount))
        assertEquals("Onion", saved.rawName)
        assertTrue(content.warnings.isEmpty())
    }

    @Test
    fun theUnitStepperStaysWithinTheLimits() {
        val viewModel = viewModel()
        viewModel.onEvent(BillReviewUiEvent.EditLine(BillLineId("l1")))
        viewModel.onEvent(BillReviewUiEvent.UnitsStepped(-5))
        assertEquals(1, viewModel.content().edit?.units)
        viewModel.onEvent(BillReviewUiEvent.UnitsStepped(10_000))
        assertEquals(BillRules.MAX_UNITS, viewModel.content().edit?.units)
    }

    @Test
    fun invalidInputShowsFieldErrorsWithoutLosingTheUsersText() {
        val viewModel = viewModel()
        viewModel.onEvent(BillReviewUiEvent.EditLine(BillLineId("l1")))
        viewModel.onEvent(BillReviewUiEvent.AmountChanged("abc"))
        viewModel.onEvent(BillReviewUiEvent.SaveLine)
        assertEquals(LineFieldError.INVALID, viewModel.content().edit?.amountError)
        assertEquals("abc", viewModel.content().edit?.amountText)
        assertTrue(repository.updates.isEmpty())

        viewModel.onEvent(BillReviewUiEvent.AmountChanged("-5"))
        viewModel.onEvent(BillReviewUiEvent.SaveLine)
        assertEquals(LineFieldError.INVALID, viewModel.content().edit?.amountError)

        viewModel.onEvent(BillReviewUiEvent.AmountChanged("40.00"))
        viewModel.onEvent(BillReviewUiEvent.NameChanged("  "))
        viewModel.onEvent(BillReviewUiEvent.SaveLine)
        val edit = assertNotNull(viewModel.content().edit)
        assertEquals(LineFieldError.REQUIRED, edit.nameError)
        assertEquals("40.00", edit.amountText)
        assertNull(edit.error)
    }

    @Test
    fun aServerFailureWhileSavingKeepsTheSheetOpenWithAMessage() {
        val viewModel = viewModel()
        viewModel.onEvent(BillReviewUiEvent.EditLine(BillLineId("l1")))
        repository.failure = AppError.Offline
        viewModel.onEvent(BillReviewUiEvent.SaveLine)
        val edit = assertNotNull(viewModel.content().edit)
        assertEquals(BillError.OFFLINE, edit.error)
        assertEquals(false, edit.isSaving)
    }

    @Test
    fun dismissingTheSheetDiscardsTheEdit() {
        val viewModel = viewModel()
        viewModel.onEvent(BillReviewUiEvent.EditLine(BillLineId("l1")))
        viewModel.onEvent(BillReviewUiEvent.NameChanged("Changed"))
        viewModel.onEvent(BillReviewUiEvent.DismissEdit)
        assertNull(viewModel.content().edit)
        assertEquals("Onion", viewModel.content().bill.lines.first().displayName)
    }

    @Test
    fun usingTheCalculatedTotalResolvesTheMismatch() {
        val viewModel = viewModel()
        viewModel.onEvent(BillReviewUiEvent.UseCalculatedTotal)
        val content = viewModel.content()
        assertEquals(Money(13_000), content.bill.total)
        assertTrue(content.warnings.isEmpty())
        assertEquals(false, content.isSaving)

        viewModel.onEvent(BillReviewUiEvent.UseCalculatedTotal)
        assertEquals(1, repository.updates.size)
    }

    @Test
    fun aFailureWhileAcceptingTheTotalShowsAMessage() {
        val viewModel = viewModel()
        repository.failure = AppError.Timeout
        viewModel.onEvent(BillReviewUiEvent.UseCalculatedTotal)
        assertEquals(BillError.OFFLINE, viewModel.content().error)
        assertEquals(false, viewModel.content().isSaving)
    }
}
