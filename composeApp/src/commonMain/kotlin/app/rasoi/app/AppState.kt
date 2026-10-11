package app.rasoi.app

import app.rasoi.core.domain.household.HouseholdSummary
import app.rasoi.core.model.Account

/** Which part of the app to show. */
sealed interface AppState {
    data object Starting : AppState

    data object SignedOut : AppState

    data class NeedsHousehold(val account: Account) : AppState

    data class Ready(val account: Account, val household: HouseholdSummary) : AppState

    data object LoadFailed : AppState
}
