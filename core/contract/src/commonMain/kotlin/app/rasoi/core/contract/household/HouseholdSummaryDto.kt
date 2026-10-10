package app.rasoi.core.contract.household

import kotlinx.serialization.Serializable

@Serializable
data class HouseholdSummaryDto(val id: String, val name: String, val timeZone: String, val role: RoleDto)
