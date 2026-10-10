package app.rasoi.core.contract.household

import kotlinx.serialization.Serializable

@Serializable
data class HouseholdDto(
    val id: String,
    val name: String,
    val timeZone: String,
    val members: List<MemberDto>,
    val locations: List<LocationDto>,
)
