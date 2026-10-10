package app.rasoi.core.domain.auth

import app.rasoi.core.model.AppResult
import app.rasoi.core.model.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val state: StateFlow<AuthState>

    /** Exchanges a verified ID token for a server session. [displayName] is required on first sign-in. */
    suspend fun signIn(idToken: String, displayName: String?): AppResult<User>

    suspend fun signOut()
}
