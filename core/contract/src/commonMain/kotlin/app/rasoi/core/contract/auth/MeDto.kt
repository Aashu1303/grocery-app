package app.rasoi.core.contract.auth

import app.rasoi.core.contract.household.HouseholdSummaryDto
import kotlinx.serialization.Serializable

@Serializable
data class MeDto(val user: UserDto, val households: List<HouseholdSummaryDto>)
