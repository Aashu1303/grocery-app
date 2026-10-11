package app.rasoi.feature.kitchen

import app.rasoi.core.domain.kitchen.DishSuggestion
import app.rasoi.core.domain.kitchen.GetUseFirstUseCase
import app.rasoi.core.domain.kitchen.SuggestDishesUseCase
import app.rasoi.core.model.AppError
import app.rasoi.core.model.Category
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Role
import app.rasoi.core.testing.Builders
import app.rasoi.core.testing.FakeKitchenRepository
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class KitchenViewModelTest : ViewModelTest() {
    private val repository = FakeKitchenRepository(
        items = listOf(
            Builders.pantryItem("p1", "Rice", Category.STAPLES, expiresOn = null),
            Builders.pantryItem("p2", "Milk", Category.DAIRY, expiresOn = LocalDate(2026, 10, 12)),
        ),
        dishes = listOf(DishSuggestion("Kheer", listOf("milk", "rice"), "Simmer rice in milk.")),
    )

    private fun viewModel(role: Role = Role.COOK) = KitchenViewModel(
        HouseholdId("h1"),
        role,
        GetUseFirstUseCase(repository),
        SuggestDishesUseCase(repository),
    )

    private fun KitchenViewModel.content() = assertIs<KitchenUiState.Content>(state.value)

    @Test
    fun listsFoodSoonestExpiryFirst() {
        val content = viewModel().content()
        assertEquals(listOf("Milk", "Rice"), content.entries.map { it.item.name })
        assertEquals(DishIdeas.Hidden, content.ideas)
    }

    @Test
    fun anEmptyPantryHasNothingToUseFirstAndNoIdeasToAskFor() {
        repository.items = emptyList()
        val viewModel = viewModel()
        assertEquals(emptyList(), viewModel.content().entries)
        viewModel.onEvent(KitchenUiEvent.AskIdeas)
        assertEquals(0, repository.suggestionCalls)
    }

    @Test
    fun aFailedLoadCanBeRetriedAndARefreshKeepsWhatIsShown() {
        repository.failure = AppError.Offline
        val viewModel = viewModel()
        assertEquals(KitchenUiState.Error(KitchenError.OFFLINE), viewModel.state.value)
        repository.failure = null
        viewModel.onEvent(KitchenUiEvent.Retry)
        assertEquals(2, viewModel.content().entries.size)
        repository.failure = AppError.Offline
        viewModel.onEvent(KitchenUiEvent.Refresh)
        assertEquals(2, viewModel.content().entries.size)
    }

    @Test
    fun askingForIdeasShowsDishesAndTheyCanBeHidden() {
        val viewModel = viewModel()
        viewModel.onEvent(KitchenUiEvent.AskIdeas)
        assertEquals(DishIdeas.Ready(repository.dishes), viewModel.content().ideas)
        viewModel.onEvent(KitchenUiEvent.Refresh)
        assertIs<DishIdeas.Ready>(viewModel.content().ideas)
        viewModel.onEvent(KitchenUiEvent.HideIdeas)
        assertEquals(DishIdeas.Hidden, viewModel.content().ideas)
    }

    @Test
    fun aServerWithoutAModelIsExplainedAndOtherFailuresCanBeTriedAgain() {
        val viewModel = viewModel()
        repository.suggestionFailure = AppError.Unavailable("suggestions_unavailable")
        viewModel.onEvent(KitchenUiEvent.AskIdeas)
        assertEquals(DishIdeas.Unavailable, viewModel.content().ideas)

        viewModel.onEvent(KitchenUiEvent.HideIdeas)
        repository.suggestionFailure = AppError.Unavailable("suggestions_failed")
        viewModel.onEvent(KitchenUiEvent.AskIdeas)
        assertEquals(DishIdeas.Failed(KitchenError.IDEAS_FAILED), viewModel.content().ideas)
        repository.suggestionFailure = null
        viewModel.onEvent(KitchenUiEvent.AskIdeas)
        assertIs<DishIdeas.Ready>(viewModel.content().ideas)
    }

    @Test
    fun errorsMapToMessages() {
        assertEquals(KitchenError.OFFLINE, AppError.Timeout.kitchenError())
        assertEquals(KitchenError.FORBIDDEN, AppError.Forbidden.kitchenError())
        assertEquals(KitchenError.GENERIC, AppError.Server(500).kitchenError())
    }
}
