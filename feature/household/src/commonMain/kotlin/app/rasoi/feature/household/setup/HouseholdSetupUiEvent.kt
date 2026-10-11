package app.rasoi.feature.household.setup

sealed interface HouseholdSetupUiEvent {
    data class NameChanged(val value: String) : HouseholdSetupUiEvent

    data object Create : HouseholdSetupUiEvent

    data class CodeChanged(val value: String) : HouseholdSetupUiEvent

    data object Join : HouseholdSetupUiEvent
}
