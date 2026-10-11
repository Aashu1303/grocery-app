package app.rasoi.server.support

import app.rasoi.core.contract.ApiHeaders
import app.rasoi.core.contract.ContractJson
import app.rasoi.core.contract.auth.CreateSessionRequestDto
import app.rasoi.core.contract.auth.SessionDto
import app.rasoi.server.AppGraph
import app.rasoi.server.AuthMode
import app.rasoi.server.ServerConfig
import app.rasoi.server.auth.DevIdTokenVerifier
import app.rasoi.server.parsing.InvoiceFallback
import app.rasoi.server.persistence.DatabaseConfig
import app.rasoi.server.rasoiModule
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.util.UUID

/** Runs [block] against the real module, a real (reset) database, dev sign-in and a controllable clock. */
fun rasoiTest(
    clock: MutableClock = MutableClock(),
    storage: InMemoryDocumentStorage = InMemoryDocumentStorage(),
    fallback: InvoiceFallback? = null,
    block: suspend TestScope.() -> Unit,
) = testApplication {
    TestPostgres.reset()
    val config = ServerConfig(
        port = 0,
        version = "test",
        database = DatabaseConfig("unused", "unused", "unused"),
        authMode = AuthMode.DEV,
        firebaseProjectId = null,
    )
    lateinit var server: Application
    application {
        rasoiModule(AppGraph(config, TestPostgres.database, DevIdTokenVerifier(), clock, storage, fallback))
        server = this
    }
    startApplication()
    TestScope(this, clock, server, storage).block()
}

class TestScope(
    private val builder: ApplicationTestBuilder,
    val clock: MutableClock,
    val application: Application,
    val storage: InMemoryDocumentStorage,
) {
    val client: HttpClient = builder.createClient { install(ContentNegotiation) { json(ContractJson) } }

    suspend fun signIn(phone: String, name: String): SessionDto = client.post("/auth/session") {
        contentType(ContentType.Application.Json)
        setBody(CreateSessionRequestDto(idToken = DevIdTokenVerifier.PREFIX + phone, displayName = name))
    }.body()
}

fun HttpRequestBuilder.authed(session: SessionDto, idempotencyKey: String? = UUID.randomUUID().toString()) {
    bearerAuth(session.token)
    contentType(ContentType.Application.Json)
    idempotencyKey?.let { header(ApiHeaders.IDEMPOTENCY_KEY, it) }
}
