package app.rasoi.server.service

import app.rasoi.core.domain.household.HouseholdRules
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.User
import app.rasoi.core.model.UserId
import app.rasoi.core.model.flatMap
import app.rasoi.core.model.map
import app.rasoi.server.auth.IdTokenVerifier
import app.rasoi.server.auth.SessionTokens
import app.rasoi.server.auth.VerifiedIdentity
import app.rasoi.server.persistence.SessionStore
import app.rasoi.server.persistence.UserStore
import kotlin.time.Clock
import kotlin.time.Duration

class AuthService(
    private val verifier: IdTokenVerifier,
    private val users: UserStore,
    private val sessions: SessionStore,
    private val tokens: SessionTokens,
    private val clock: Clock,
    private val sessionTtl: Duration,
) {
    /** Verifies [idToken], creates the user on first sign-in (which needs [displayName]) and issues a session. */
    suspend fun signIn(idToken: String, displayName: String?): AppResult<Session> =
        verifier.verify(idToken).flatMap { identity -> findOrCreate(identity, displayName) }.map { user ->
            val now = clock.now()
            val token = tokens.newToken()
            val expiresAt = now + sessionTtl
            sessions.create(tokens.hash(token), user.id, now, expiresAt)
            Session(token, expiresAt, user)
        }

    suspend fun authenticate(token: String): UserId? = sessions.userFor(tokens.hash(token), clock.now())

    private suspend fun findOrCreate(identity: VerifiedIdentity, displayName: String?): AppResult<User> {
        users.findByFirebaseUid(identity.firebaseUid)?.let { return AppResult.Success(it) }
        if (displayName == null) {
            return AppResult.Failure(
                AppError.Validation(listOf(AppError.FieldViolation("displayName", "required"))),
            )
        }
        return HouseholdRules.validateDisplayName(displayName).map { name ->
            users.create(identity.firebaseUid, identity.phone, name, clock.now())
        }
    }
}
