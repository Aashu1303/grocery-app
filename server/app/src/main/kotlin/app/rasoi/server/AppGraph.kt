package app.rasoi.server

import app.rasoi.core.domain.household.InviteCodeGenerator
import app.rasoi.server.auth.DevIdTokenVerifier
import app.rasoi.server.auth.FirebaseIdTokenVerifier
import app.rasoi.server.auth.IdTokenVerifier
import app.rasoi.server.auth.SessionTokens
import app.rasoi.server.http.Idempotency
import app.rasoi.server.parsing.InvoiceFallback
import app.rasoi.server.parsing.InvoicePipeline
import app.rasoi.server.parsing.fallback.LlmInvoiceFallback
import app.rasoi.server.parsing.fallback.OpenAiChatClient
import app.rasoi.server.persistence.BillStore
import app.rasoi.server.persistence.HouseholdStore
import app.rasoi.server.persistence.IdempotencyStore
import app.rasoi.server.persistence.InviteStore
import app.rasoi.server.persistence.LocationStore
import app.rasoi.server.persistence.PantryStore
import app.rasoi.server.persistence.RasoiDatabase
import app.rasoi.server.persistence.SessionStore
import app.rasoi.server.persistence.UserStore
import app.rasoi.server.service.AuthService
import app.rasoi.server.service.BillService
import app.rasoi.server.service.HouseholdService
import app.rasoi.server.service.InvoiceImportService
import app.rasoi.server.service.PantryPlanner
import app.rasoi.server.service.PantryService
import app.rasoi.server.storage.DocumentStorage
import app.rasoi.server.storage.FileDocumentStorage
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import java.net.URI
import java.nio.file.Path
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import kotlin.random.asKotlinRandom
import kotlin.time.Clock

/** Composition root for the server (ADR 0015): plain constructors, wired once. */
class AppGraph(
    val config: ServerConfig,
    db: RasoiDatabase,
    verifier: IdTokenVerifier,
    clock: Clock,
    storage: DocumentStorage = FileDocumentStorage(Path.of(config.storageDir)),
    fallback: InvoiceFallback? = fallbackFor(config),
) {
    private val users = UserStore(db)

    private val locationStore = LocationStore(db)

    val auth = AuthService(verifier, users, SessionStore(db), SessionTokens(), clock, config.sessionTtl)

    private val householdStore = HouseholdStore(db)

    val households = HouseholdService(
        users = users,
        households = householdStore,
        invites = InviteStore(db),
        locations = locationStore,
        codes = InviteCodeGenerator(SecureRandom().asKotlinRandom()),
        clock = clock,
    )

    private val billStore = BillStore(db)

    private val pantryStore = PantryStore(db)

    private val planner = PantryPlanner(locationStore)

    val imports =
        InvoiceImportService(households, billStore, storage, InvoicePipeline.default(fallback), planner, clock)

    val bills = BillService(households, billStore, pantryStore, planner, clock)

    val pantry = PantryService(households, householdStore, pantryStore, locationStore, clock)

    internal val idempotency = Idempotency(IdempotencyStore(db), clock)

    companion object {
        private const val GOOGLE_KEYS =
            "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"
        private const val KEY_CACHE_SIZE = 10L
        private const val KEY_CACHE_HOURS = 6L
        private const val LLM_TIMEOUT_MILLIS = 60_000L

        fun fallbackFor(config: ServerConfig): InvoiceFallback? = config.llm?.let { llm ->
            val http = HttpClient(OkHttp) { install(HttpTimeout) { requestTimeoutMillis = LLM_TIMEOUT_MILLIS } }
            LlmInvoiceFallback(OpenAiChatClient(http, llm))
        }

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
