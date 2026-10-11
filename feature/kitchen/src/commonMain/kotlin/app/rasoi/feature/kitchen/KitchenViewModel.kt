package app.rasoi.feature.kitchen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rasoi.core.domain.kitchen.GetUseFirstUseCase
import app.rasoi.core.domain.kitchen.SuggestDishesUseCase
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/** What to cook first, and on request what to cook with it. */
@KoinViewModel
class KitchenViewModel(
    @InjectedParam private val householdId: HouseholdId,
    @InjectedParam private val myRole: Role,
    private val getUseFirst: GetUseFirstUseCase,
    private val suggestDishes: SuggestDishesUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow<KitchenUiState>(KitchenUiState.Loading)
    val state: StateFlow<KitchenUiState> = mutableState.asStateFlow()

    init {
        load(showLoading = true)
    }

    fun onEvent(event: KitchenUiEvent) {
        when (event) {
            KitchenUiEvent.Retry -> load(showLoading = true)
            KitchenUiEvent.Refresh -> if (mutableState.value != KitchenUiState.Loading) load(showLoading = false)
            KitchenUiEvent.AskIdeas -> askIdeas()
            KitchenUiEvent.HideIdeas -> updateContent { it.copy(ideas = DishIdeas.Hidden) }
        }
    }

    private fun load(showLoading: Boolean) {
        if (showLoading) mutableState.value = KitchenUiState.Loading
        viewModelScope.launch {
            when (val result = getUseFirst(householdId, myRole)) {
                is AppResult.Success -> mutableState.update { previous ->
                    KitchenUiState.Content(
                        result.value,
                        (previous as? KitchenUiState.Content)?.ideas ?: DishIdeas.Hidden,
                    )
                }
                is AppResult.Failure -> if (showLoading) {
                    mutableState.value =
                        KitchenUiState.Error(result.error.kitchenError())
                }
            }
        }
    }

    private fun askIdeas() {
        val content = mutableState.value as? KitchenUiState.Content ?: return
        if (content.ideas == DishIdeas.Loading || content.entries.isEmpty()) return
        updateContent { it.copy(ideas = DishIdeas.Loading) }
        viewModelScope.launch {
            val ideas = when (val result = suggestDishes(householdId, myRole)) {
                is AppResult.Success -> DishIdeas.Ready(result.value)
                is AppResult.Failure -> result.error.asIdeas()
            }
            updateContent { it.copy(ideas = ideas) }
        }
    }

    private fun AppError.asIdeas(): DishIdeas = if (this ==
        AppError.Unavailable("suggestions_unavailable")
    ) {
        DishIdeas.Unavailable
    } else {
        DishIdeas.Failed(kitchenError())
    }

    private fun updateContent(transform: (KitchenUiState.Content) -> KitchenUiState.Content) {
        mutableState.update { state -> if (state is KitchenUiState.Content) transform(state) else state }
    }
}
