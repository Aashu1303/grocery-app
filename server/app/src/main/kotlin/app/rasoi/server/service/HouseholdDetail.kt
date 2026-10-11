package app.rasoi.server.service

import app.rasoi.core.model.Household
import app.rasoi.core.model.Location
import app.rasoi.core.model.Member

data class HouseholdDetail(val household: Household, val members: List<Member>, val locations: List<Location>)
