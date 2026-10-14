package app.rasoi.feature.household.detail

import app.rasoi.core.domain.household.HouseholdDetails
import app.rasoi.core.model.LocationKind
import app.rasoi.core.model.Role
import app.rasoi.feature.household.FieldError
import app.rasoi.feature.household.ScreenError

sealed interface HouseholdUiState {
    data object Loading : HouseholdUiState

    data class Error(val error: ScreenError) : HouseholdUiState

    data class Content(
        val details: HouseholdDetails,
        val myRole: Role,
        val canInvite: Boolean,
        val canAddLocation: Boolean,
        val invite: InviteSheetState? = null,
        val addLocation: AddLocationState? = null,
        /** The user pulled to refresh; the content stays while it reloads. */
        val isRefreshing: Boolean = false,
    ) : HouseholdUiState
}

data class InviteSheetState(
    val role: Role = Role.MEMBER,
    val code: String? = null,
    val isCreating: Boolean = false,
    val error: ScreenError? = null,
)

data class AddLocationState(
    val name: String = "",
    val kind: LocationKind = LocationKind.SHELF,
    val nameError: FieldError? = null,
    val error: ScreenError? = null,
    val isSaving: Boolean = false,
)
