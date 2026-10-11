package app.rasoi.core.domain.kitchen

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Category
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Role
import app.rasoi.core.testing.Builders
import app.rasoi.core.testing.FakeKitchenRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class KitchenUseCasesTest {
    private val household = HouseholdId("h1")
    private val repository = FakeKitchenRepository(
        items = listOf(Builders.pantryItem("p1", "Milk", Category.DAIRY, expiresOn = LocalDate(2026, 10, 12))),
        dishes = listOf(DishSuggestion("Kheer", listOf("milk"), "Simmer.")),
    )

    @Test
    fun everyRoleThatViewsThePantryCanSeeUseFirstAndSuggestions() = runTest {
        listOf(Role.OWNER, Role.MEMBER, Role.COOK).forEach { role ->
            val entries = (GetUseFirstUseCase(repository)(household, role) as AppResult.Success).value
            assertEquals(listOf("Milk"), entries.map { it.item.name })
            assertEquals(
                listOf("Kheer"),
                (SuggestDishesUseCase(repository)(household, role) as AppResult.Success).value.map {
                    it.name
                },
            )
        }
    }

    @Test
    fun failuresPassThroughUntouched() = runTest {
        repository.suggestionFailure = AppError.Unavailable("suggestions_unavailable")
        assertEquals(
            AppResult.Failure(AppError.Unavailable("suggestions_unavailable")),
            SuggestDishesUseCase(repository)(household, Role.MEMBER),
        )
    }
}
