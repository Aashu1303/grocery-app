package app.rasoi.core.model

import kotlin.time.Instant

/** A single-use invitation to join [householdId] with [role] until [expiresAt]. */
data class Invite(
    val code: InviteCode,
    val householdId: HouseholdId,
    val role: Role,
    val expiresAt: Instant,
    val acceptedBy: UserId? = null,
)
