package app.rasoi.feature.household.setup

import app.rasoi.core.model.HouseholdId

sealed interface HouseholdSetupEffect {
    data class HouseholdReady(val householdId: HouseholdId) : HouseholdSetupEffect
}
