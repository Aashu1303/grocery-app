package app.rasoi.core.contract.household

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class InviteDto(val code: String, val householdId: String, val role: RoleDto, val expiresAt: Instant)
