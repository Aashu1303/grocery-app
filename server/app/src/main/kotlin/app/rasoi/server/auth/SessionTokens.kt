package app.rasoi.server.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** Opaque bearer tokens: 256 random bits for the client, and only their SHA-256 hash stored server-side. */
class SessionTokens(private val random: SecureRandom = SecureRandom()) {
    fun newToken(): String {
        val bytes = ByteArray(TOKEN_BYTES).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hash(token: String): String = MessageDigest.getInstance("SHA-256")
        .digest(token.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private companion object {
        const val TOKEN_BYTES = 32
    }
}
