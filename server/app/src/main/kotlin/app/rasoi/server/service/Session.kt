package app.rasoi.server.service

import app.rasoi.core.model.User
import kotlin.time.Instant

/** A freshly issued session; [token] is shown to the client once and only its hash is stored. */
data class Session(val token: String, val expiresAt: Instant, val user: User)
