package app.rasoi.feature.kitchen

sealed interface KitchenUiEvent {
    data object Retry : KitchenUiEvent

    /** The user pulled the screen down: reload, showing the refresh indicator until it is done. */
    data object PullToRefresh : KitchenUiEvent

    /** Reloads quietly when the screen comes back. */
    data object Refresh : KitchenUiEvent

    data object AskIdeas : KitchenUiEvent

    data object HideIdeas : KitchenUiEvent
}
