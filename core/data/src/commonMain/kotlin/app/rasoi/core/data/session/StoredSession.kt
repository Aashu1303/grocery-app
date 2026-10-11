package app.rasoi.core.data.session

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** What the app keeps between launches. The token is a secret: only [SessionStorage] persists it. */
@Serializable
data class StoredSession(val token: String, val expiresAt: Instant, val userId: String, val displayName: String) {
    override fun toString(): String = "StoredSession(userId=$userId, expiresAt=$expiresAt)"
}
