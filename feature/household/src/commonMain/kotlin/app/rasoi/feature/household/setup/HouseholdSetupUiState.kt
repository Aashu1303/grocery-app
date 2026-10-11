package app.rasoi.feature.household.setup

import app.rasoi.feature.household.FieldError
import app.rasoi.feature.household.ScreenError

data class HouseholdSetupUiState(
    val name: String = "",
    val code: String = "",
    val nameError: FieldError? = null,
    val codeError: FieldError? = null,
    val error: ScreenError? = null,
    val isCreating: Boolean = false,
    val isJoining: Boolean = false,
) {
    val isBusy: Boolean get() = isCreating || isJoining
}
