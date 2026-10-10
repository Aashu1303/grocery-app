package app.rasoi.core.contract.household

import kotlinx.serialization.Serializable

@Serializable
data class MemberDto(val userId: String, val displayName: String, val role: RoleDto)
