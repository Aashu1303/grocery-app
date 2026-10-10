package app.rasoi.core.contract.auth

import kotlinx.serialization.Serializable

/** Exchange a Firebase ID token for a Rasoi session. [displayName] is required the first time a user signs in. */
@Serializable
data class CreateSessionRequestDto(val idToken: String, val displayName: String? = null)
