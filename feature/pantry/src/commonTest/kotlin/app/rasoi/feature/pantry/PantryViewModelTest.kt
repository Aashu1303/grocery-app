package app.rasoi.feature.pantry

import app.rasoi.core.domain.pantry.AddPantryItemUseCase
import app.rasoi.core.domain.pantry.InputUnit
import app.rasoi.core.domain.pantry.ListPantryUseCase
import app.rasoi.core.domain.pantry.UpdatePantryItemUseCase
import app.rasoi.core.model.AppError
import app.rasoi.core.model.Category
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.LocationId
import app.rasoi.core.model.PantryItemId
import app.rasoi.core.model.PantryStatus
import app.rasoi.core.model.Quantity
import app.rasoi.core.model.Role
import app.rasoi.core.testing.Builders
import app.rasoi.core.testing.FakeHouseholdRepository
import app.rasoi.core.testing.FakePantryRepository
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class PantryViewModelTest : ViewModelTest() {
    private val clock = object : Clock {
        override fun now() = Instant.parse("2026-10-11T04:30:00Z")
    }
    private val households = FakeHouseholdRepository(
        details = Builders.details(
            locations = listOf(Builders.location("l1", "Fridge"), Builders.location("l2", "Shelf")),
        ),
    )
    private val pantry = FakePantryRepository(
        listOf(
            Builders.pantryItem("p1", "Rice", locationId = "l2"),
            Builders.pantryItem("p2", "Milk", locationId = "l1", expiresOn = LocalDate(2026, 10, 12)),
            Builders.pantryItem("p3", "Beans", locationId = "l1", expiresOn = LocalDate(2026, 10, 10)),
            Builders.pantryItem("p4", "Old jam", locationId = "l2", status = PantryStatus.FINISHED),
        ),
    )

    private fun viewModel(role: Role = Role.MEMBER) = PantryViewModel(
        HouseholdId("h1"),
        role,
        households,
        ListPantryUseCase(pantry),
        AddPantryItemUseCase(pantry),
        UpdatePantryItemUseCase(pantry),
        clock,
    )

    private fun PantryViewModel.content() = assertIs<PantryUiState.Content>(state.value)

    private fun PantryViewModel.sheet() = content().sheet

    @Test
    fun loadsInStockItemsSoonestExpiryFirstWithTodayInTheHouseholdZone() {
        val content = viewModel().content()
        assertEquals(listOf("Beans", "Milk", "Rice"), content.items.map { it.name })
        assertEquals(LocalDate(2026, 10, 11), content.today)
        assertEquals(listOf("Fridge", "Shelf"), content.locations.map { it.name })
        assertTrue(content.canEdit)
    }

    @Test
    fun cooksCanEditThePantryToo() {
        assertTrue(viewModel(Role.COOK).content().canEdit)
    }

    @Test
    fun loadFailuresShowAnErrorAndRetryRecovers() {
        pantry.failure = AppError.Offline
        val viewModel = viewModel()
        assertEquals(PantryUiState.Error(PantryError.OFFLINE), viewModel.state.value)
        pantry.failure = null
        households.failure = AppError.Timeout
        viewModel.onEvent(PantryUiEvent.Retry)
        assertEquals(PantryUiState.Error(PantryError.OFFLINE), viewModel.state.value)
        households.failure = null
        viewModel.onEvent(PantryUiEvent.Retry)
        viewModel.content()
    }

    @Test
    fun filteringShowsOnlyOneStoragePlace() {
        val viewModel = viewModel()
        viewModel.onEvent(PantryUiEvent.FilterSelected(LocationId("l1")))
        assertEquals(listOf("Beans", "Milk"), viewModel.content().visibleItems.map { it.name })
        viewModel.onEvent(PantryUiEvent.FilterSelected(null))
        assertEquals(3, viewModel.content().visibleItems.size)
    }

    @Test
    fun anInvalidAmountOrNameStaysOnTheFormWithFieldErrors() {
        val viewModel = viewModel()
        viewModel.onEvent(PantryUiEvent.OpenAdd)
        viewModel.onEvent(PantryUiEvent.Form.NameChanged("Paneer"))
        viewModel.onEvent(PantryUiEvent.Form.AmountChanged("lots"))
        viewModel.onEvent(PantryUiEvent.SaveNew)
        val form = assertIs<PantrySheet.Add>(viewModel.sheet()).form
        assertEquals(PantryFieldError.INVALID, form.amountError)
        assertEquals("lots", form.amountText)
        assertTrue(pantry.added.isEmpty())

        viewModel.onEvent(PantryUiEvent.Form.AmountChanged("200"))
        viewModel.onEvent(PantryUiEvent.Form.NameChanged(" "))
        viewModel.onEvent(PantryUiEvent.SaveNew)
        assertEquals(PantryFieldError.REQUIRED, assertIs<PantrySheet.Add>(viewModel.sheet()).form.nameError)
    }

    @Test
    fun addingAnItemCategorizesItAndShowsItInTheList() {
        val viewModel = viewModel()
        viewModel.onEvent(PantryUiEvent.OpenAdd)
        viewModel.onEvent(PantryUiEvent.Form.NameChanged("Paneer"))
        viewModel.onEvent(PantryUiEvent.Form.AmountChanged("250"))
        viewModel.onEvent(PantryUiEvent.Form.UnitSelected(InputUnit.GRAMS))
        viewModel.onEvent(PantryUiEvent.Form.LocationSelected(LocationId("l1")))
        viewModel.onEvent(PantryUiEvent.SaveNew)

        val added = pantry.added.single()
        assertEquals(Category.DAIRY, added.category)
        assertEquals(Quantity.grams(250), added.quantity)
        assertEquals(LocationId("l1"), added.locationId)
        assertNull(viewModel.sheet())
        assertTrue(viewModel.content().items.any { it.name == "Paneer" })
    }

    @Test
    fun aFailedAddKeepsTheFormWithAMessage() {
        val viewModel = viewModel()
        viewModel.onEvent(PantryUiEvent.OpenAdd)
        viewModel.onEvent(PantryUiEvent.Form.NameChanged("Paneer"))
        pantry.failure = AppError.Offline
        viewModel.onEvent(PantryUiEvent.SaveNew)
        val form = assertIs<PantrySheet.Add>(viewModel.sheet()).form
        assertEquals(PantryError.OFFLINE, form.error)
        assertEquals("Paneer", form.name)
        assertEquals(false, form.isSaving)
    }

    @Test
    fun movingAnItemSavesItAndKeepsTheSheetOpen() {
        val viewModel = viewModel()
        viewModel.onEvent(PantryUiEvent.OpenItem(PantryItemId("p1")))
        viewModel.onEvent(PantryUiEvent.Change.MoveTo(LocationId("l1")))
        assertEquals(LocationId("l1"), viewModel.content().items.first { it.id.value == "p1" }.locationId)
        assertIs<PantrySheet.Details>(viewModel.sheet())
    }

    @Test
    fun steppingTheExpiryStartsFromTodayWhenThereIsNone() {
        val viewModel = viewModel()
        viewModel.onEvent(PantryUiEvent.OpenItem(PantryItemId("p1")))
        viewModel.onEvent(PantryUiEvent.Change.ExpiryStepped(3))
        assertEquals(LocalDate(2026, 10, 14), viewModel.content().items.first { it.id.value == "p1" }.expiresOn)
        viewModel.onEvent(PantryUiEvent.Change.ExpiryStepped(-1))
        assertEquals(LocalDate(2026, 10, 13), viewModel.content().items.first { it.id.value == "p1" }.expiresOn)
    }

    @Test
    fun finishingAnItemRemovesItAndClosesTheSheet() {
        val viewModel = viewModel()
        viewModel.onEvent(PantryUiEvent.OpenItem(PantryItemId("p2")))
        viewModel.onEvent(PantryUiEvent.Change.MarkFinished)
        assertEquals(listOf("Beans", "Rice"), viewModel.content().items.map { it.name })
        assertNull(viewModel.sheet())
    }

    @Test
    fun discardingNeedsConfirmationFirst() {
        val viewModel = viewModel()
        viewModel.onEvent(PantryUiEvent.OpenItem(PantryItemId("p2")))
        viewModel.onEvent(PantryUiEvent.Change.AskDiscard)
        assertEquals(PantrySheet.ConfirmDiscard(PantryItemId("p2")), viewModel.sheet())
        assertTrue(pantry.updates.isEmpty())

        viewModel.onEvent(PantryUiEvent.DismissSheet)
        assertEquals(3, viewModel.content().items.size)

        viewModel.onEvent(PantryUiEvent.OpenItem(PantryItemId("p2")))
        viewModel.onEvent(PantryUiEvent.Change.AskDiscard)
        viewModel.onEvent(PantryUiEvent.Change.ConfirmDiscard)
        assertEquals(PantryStatus.DISCARDED, pantry.items.first { it.id.value == "p2" }.status)
        assertEquals(listOf("Beans", "Rice"), viewModel.content().items.map { it.name })
        assertNull(viewModel.sheet())
    }

    @Test
    fun aFailedChangeShowsAMessageInTheSheet() {
        val viewModel = viewModel()
        viewModel.onEvent(PantryUiEvent.OpenItem(PantryItemId("p1")))
        pantry.failure = AppError.Offline
        viewModel.onEvent(PantryUiEvent.Change.MarkFinished)
        val details = assertNotNull(viewModel.sheet() as? PantrySheet.Details)
        assertEquals(PantryError.OFFLINE, details.error)
        assertEquals(false, details.isSaving)
        assertEquals(3, viewModel.content().items.size)
    }

    @Test
    fun changesWithoutAnOpenItemDoNothing() {
        val viewModel = viewModel()
        viewModel.onEvent(PantryUiEvent.Change.MarkFinished)
        viewModel.onEvent(PantryUiEvent.Change.ExpiryStepped(1))
        viewModel.onEvent(PantryUiEvent.Change.ConfirmDiscard)
        assertTrue(pantry.updates.isEmpty())
    }

    @Test
    fun everyErrorHasAMessage() {
        assertEquals(PantryError.entries.toSet(), pantryErrorMessages.keys)
    }
}
