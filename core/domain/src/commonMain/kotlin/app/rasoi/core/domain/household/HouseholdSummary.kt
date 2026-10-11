package app.rasoi.core.domain.household

import app.rasoi.core.model.Household
import app.rasoi.core.model.Role

/** A household the signed-in user belongs to, with their role in it. */
data class HouseholdSummary(val household: Household, val role: Role)
