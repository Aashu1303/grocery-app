package app.rasoi.core.contract.auth

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(val id: String, val displayName: String, val upiVpa: String? = null)
