package app.rasoi.core.contract.household

import kotlinx.serialization.Serializable

@Serializable
data class CreateHouseholdRequestDto(val name: String, val timeZone: String)
