package app.rasoi.core.model

/** A household member as other members see them. */
data class Member(val userId: UserId, val displayName: String, val role: Role)
