package app.rasoi.feature.household.detail

import app.rasoi.core.model.LocationKind
import app.rasoi.core.model.Role

sealed interface HouseholdUiEvent {
    data object Retry : HouseholdUiEvent

    /** The user pulled the screen down: reload, showing the refresh indicator until it is done. */
    data object PullToRefresh : HouseholdUiEvent

    data object OpenInvite : HouseholdUiEvent

    data class InviteRoleSelected(val role: Role) : HouseholdUiEvent

    data object CreateInvite : HouseholdUiEvent

    data object DismissInvite : HouseholdUiEvent

    data object OpenAddLocation : HouseholdUiEvent

    data class LocationNameChanged(val value: String) : HouseholdUiEvent

    data class LocationKindSelected(val kind: LocationKind) : HouseholdUiEvent

    data object SaveLocation : HouseholdUiEvent

    data object DismissAddLocation : HouseholdUiEvent
}
