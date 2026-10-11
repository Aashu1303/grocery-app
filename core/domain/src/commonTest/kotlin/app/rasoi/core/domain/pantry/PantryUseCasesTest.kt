package app.rasoi.core.domain.pantry

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Category
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.LocationId
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.PantryItemId
import app.rasoi.core.model.PantryStatus
import app.rasoi.core.model.Quantity
import app.rasoi.core.model.Role
import app.rasoi.core.testing.Builders
import app.rasoi.core.testing.FakePantryRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PantryUseCasesTest {
    private val repository = FakePantryRepository(listOf(Builders.pantryItem("p1"), Builders.pantryItem("p2", "Milk")))
    private val household = HouseholdId("h1")
    private val draft = NewPantryItem("Rice", Category.STAPLES, Quantity.kilograms(1), LocationId("l1"), null)

    private fun invalid(field: String, code: String) =
        AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation(field, code))))

    @Test
    fun listReturnsTheHouseholdsItemsForAStatus() = runTest {
        val items = assertIs<AppResult.Success<List<PantryItem>>>(ListPantryUseCase(repository)(household)).value
        assertEquals(listOf("p1", "p2"), items.map { it.id.value })
        repository.items[0] = repository.items[0].copy(status = PantryStatus.FINISHED)
        val inStock = ListPantryUseCase(repository)(household, PantryStatus.IN_STOCK)
        assertEquals(listOf("p2"), (inStock as AppResult.Success).value.map { it.id.value })
    }

    @Test
    fun addChecksRoleAndInputBeforeCalling() = runTest {
        val add = AddPantryItemUseCase(repository)
        assertIs<AppResult.Success<PantryItem>>(add(household, Role.COOK, draft))
        assertIs<AppResult.Success<PantryItem>>(add(household, Role.MEMBER, draft))
        val calls = repository.added.size
        assertEquals(invalid("name", "required"), add(household, Role.OWNER, draft.copy(name = " ")))
        assertEquals(calls, repository.added.size)
    }

    @Test
    fun updateValidatesLocallyBeforeSaving() = runTest {
        val update = UpdatePantryItemUseCase(repository)
        assertEquals(invalid("body", "empty"), update(household, PantryItemId("p1"), PantryEdit()))
        assertTrue(repository.updates.isEmpty())
        val result = update(household, PantryItemId("p1"), PantryEdit(status = PantryStatus.FINISHED))
        assertEquals(PantryStatus.FINISHED, (result as AppResult.Success).value.status)
        assertEquals(1, repository.updates.size)
    }

    @Test
    fun failuresComeBackAsTypedErrors() = runTest {
        repository.failure = AppError.Offline
        assertEquals(AppResult.Failure(AppError.Offline), ListPantryUseCase(repository)(household))
    }
}
