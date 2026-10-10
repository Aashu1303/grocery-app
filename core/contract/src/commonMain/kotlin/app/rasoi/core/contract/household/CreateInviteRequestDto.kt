package app.rasoi.core.contract.household

import kotlinx.serialization.Serializable

@Serializable
data class CreateInviteRequestDto(val role: RoleDto)
