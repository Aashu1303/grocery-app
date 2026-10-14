package app.rasoi.feature.kitchen

import app.rasoi.core.domain.kitchen.DishSuggestion
import app.rasoi.core.domain.kitchen.UseFirstEntry

sealed interface KitchenUiState {
    data object Loading : KitchenUiState

    data class Error(val error: KitchenError) : KitchenUiState

    data class Content(
        val entries: List<UseFirstEntry>,
        val ideas: DishIdeas = DishIdeas.Hidden,
        val isRefreshing: Boolean = false,
    ) : KitchenUiState
}

/** The "what can I cook?" section under the use-first list. */
sealed interface DishIdeas {
    data object Hidden : DishIdeas

    data object Loading : DishIdeas

    data class Ready(val dishes: List<DishSuggestion>) : DishIdeas

    /** The server has no chat model. */
    data object Unavailable : DishIdeas

    data class Failed(val error: KitchenError) : DishIdeas
}
