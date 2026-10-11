package app.rasoi.server

import app.rasoi.core.domain.household.InviteCodeGenerator
import app.rasoi.server.auth.DevIdTokenVerifier
import app.rasoi.server.auth.FirebaseIdTokenVerifier
import app.rasoi.server.auth.IdTokenVerifier
import app.rasoi.server.auth.SessionTokens
import app.rasoi.server.http.Idempotency
import app.rasoi.server.notifications.FcmNotifier
import app.rasoi.server.notifications.NoopNotifier
import app.rasoi.server.notifications.Notifier
import app.rasoi.server.notifications.ServiceAccount
import app.rasoi.server.parsing.InvoiceFallback
import app.rasoi.server.parsing.InvoicePipeline
import app.rasoi.server.parsing.fallback.ChatCompletionClient
import app.rasoi.server.parsing.fallback.LlmInvoiceFallback
import app.rasoi.server.parsing.fallback.OpenAiChatClient
import app.rasoi.server.persistence.AlertStore
import app.rasoi.server.persistence.BillStore
import app.rasoi.server.persistence.DeviceStore
import app.rasoi.server.persistence.HouseholdStore
import app.rasoi.server.persistence.IdempotencyStore
import app.rasoi.server.persistence.InviteStore
import app.rasoi.server.persistence.LocationStore
import app.rasoi.server.persistence.PantryStore
import app.rasoi.server.persistence.PeriodStore
import app.rasoi.server.persistence.RasoiDatabase
import app.rasoi.server.persistence.SessionStore
import app.rasoi.server.persistence.UserStore
import app.rasoi.server.service.AuthService
import app.rasoi.server.service.BillService
import app.rasoi.server.service.DeviceService
import app.rasoi.server.service.ExpiryAlertService
import app.rasoi.server.service.HouseholdService
import app.rasoi.server.service.InvoiceImportService
import app.rasoi.server.service.KitchenService
import app.rasoi.server.service.NotificationService
import app.rasoi.server.service.PantryPlanner
import app.rasoi.server.service.PantryService
import app.rasoi.server.service.PeriodService
import app.rasoi.server.service.ProfileService
import app.rasoi.server.storage.DocumentStorage
import app.rasoi.server.storage.FileDocumentStorage
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import java.io.File
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
    chat: ChatCompletionClient? = chatFor(config),
    fallback: InvoiceFallback? = chat?.let(::LlmInvoiceFallback),
    notifier: Notifier = notifierFor(config, clock),
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

    val periods = PeriodService(households, PeriodStore(db), billStore, users, clock)

    val profile = ProfileService(users)

    val imports =
        InvoiceImportService(households, billStore, storage, InvoicePipeline.default(fallback), planner, periods, clock)

    private val deviceStore = DeviceStore(db)

    private val notifications = NotificationService(notifier, deviceStore)

    val bills = BillService(households, billStore, pantryStore, planner, periods, notifications, clock)

    val devices = DeviceService(deviceStore, clock)

    val kitchen = KitchenService(households, pantryStore, chat, clock)

    val expiryAlerts = ExpiryAlertService(pantryStore, AlertStore(db), householdStore, notifications, clock)

    val pantry = PantryService(households, householdStore, pantryStore, locationStore, clock)

    internal val idempotency = Idempotency(IdempotencyStore(db), clock)

    companion object {
        private const val GOOGLE_KEYS =
            "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"
        private const val KEY_CACHE_SIZE = 10L
        private const val KEY_CACHE_HOURS = 6L
        private const val LLM_TIMEOUT_MILLIS = 60_000L

        /** The one chat model behind invoice fallback and dish suggestions; off unless all `RASOI_LLM_*` are set. */
        fun chatFor(config: ServerConfig): ChatCompletionClient? = config.llm?.let { llm ->
            val http = HttpClient(OkHttp) { install(HttpTimeout) { requestTimeoutMillis = LLM_TIMEOUT_MILLIS } }
            OpenAiChatClient(http, llm)
        }

        /** FCM when a usable service-account file is configured, otherwise pushes are quietly skipped. */
        fun notifierFor(config: ServerConfig, clock: Clock): Notifier {
            val account = config.fcmServiceAccountFile
                ?.let { runCatching { File(it).readText() }.getOrNull() }
                ?.let(ServiceAccount::parse)
            return account?.let { FcmNotifier(HttpClient(OkHttp), it, clock) } ?: NoopNotifier
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
