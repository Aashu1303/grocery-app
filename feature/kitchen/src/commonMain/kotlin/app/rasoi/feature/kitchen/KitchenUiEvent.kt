package app.rasoi.feature.kitchen

sealed interface KitchenUiEvent {
    data object Retry : KitchenUiEvent

    /** Reloads quietly when the screen comes back. */
    data object Refresh : KitchenUiEvent

    data object AskIdeas : KitchenUiEvent

    data object HideIdeas : KitchenUiEvent
}
