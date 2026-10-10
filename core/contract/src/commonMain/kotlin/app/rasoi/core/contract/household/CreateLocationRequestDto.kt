package app.rasoi.core.contract.household

import kotlinx.serialization.Serializable

@Serializable
data class CreateLocationRequestDto(val name: String, val kind: LocationKindDto)
