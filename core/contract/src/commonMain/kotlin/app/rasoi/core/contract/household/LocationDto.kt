package app.rasoi.core.contract.household

import kotlinx.serialization.Serializable

@Serializable
data class LocationDto(val id: String, val name: String, val kind: LocationKindDto)
