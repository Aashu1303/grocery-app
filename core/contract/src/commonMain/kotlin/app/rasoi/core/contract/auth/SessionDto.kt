package app.rasoi.core.contract.auth

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class SessionDto(val token: String, val expiresAt: Instant, val user: UserDto)
