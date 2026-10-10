package app.rasoi.server

import app.rasoi.core.domain.household.InviteCodeGenerator
import app.rasoi.server.auth.DevIdTokenVerifier
import app.rasoi.server.auth.FirebaseIdTokenVerifier
import app.rasoi.server.auth.IdTokenVerifier
import app.rasoi.server.auth.SessionTokens
import app.rasoi.server.http.Idempotency
import app.rasoi.server.persistence.HouseholdStore
import app.rasoi.server.persistence.IdempotencyStore
import app.rasoi.server.persistence.InviteStore
import app.rasoi.server.persistence.LocationStore
import app.rasoi.server.persistence.RasoiDatabase
import app.rasoi.server.persistence.SessionStore
import app.rasoi.server.persistence.UserStore
import app.rasoi.server.service.AuthService
import app.rasoi.server.service.HouseholdService
import com.auth0.jwk.JwkProviderBuilder
import java.net.URI
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import kotlin.random.asKotlinRandom
import kotlin.time.Clock

/** Composition root for the server (ADR 0015): plain constructors, wired once. */
class AppGraph(val config: ServerConfig, db: RasoiDatabase, verifier: IdTokenVerifier, clock: Clock) {
    private val users = UserStore(db)

    val auth = AuthService(verifier, users, SessionStore(db), SessionTokens(), clock, config.sessionTtl)

    val households = HouseholdService(
        users = users,
        households = HouseholdStore(db),
        invites = InviteStore(db),
        locations = LocationStore(db),
        codes = InviteCodeGenerator(SecureRandom().asKotlinRandom()),
        clock = clock,
    )

    internal val idempotency = Idempotency(IdempotencyStore(db), clock)

    companion object {
        private const val GOOGLE_KEYS =
            "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"
        private const val KEY_CACHE_SIZE = 10L
        private const val KEY_CACHE_HOURS = 6L

        fun verifierFor(config: ServerConfig, clock: Clock): IdTokenVerifier = when (config.authMode) {
            AuthMode.DEV -> DevIdTokenVerifier()
            AuthMode.FIREBASE -> FirebaseIdTokenVerifier(
                projectId = requireNotNull(config.firebaseProjectId),
                keys = JwkProviderBuilder(URI(GOOGLE_KEYS).toURL())
                    .cached(KEY_CACHE_SIZE, KEY_CACHE_HOURS, TimeUnit.HOURS)
                    .build(),
                clock = clock,
            )
        }
    }
}
