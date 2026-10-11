package app.rasoi.core.domain.household

import app.rasoi.core.model.Household
import app.rasoi.core.model.Location
import app.rasoi.core.model.Member

data class HouseholdDetails(val household: Household, val members: List<Member>, val locations: List<Location>)
