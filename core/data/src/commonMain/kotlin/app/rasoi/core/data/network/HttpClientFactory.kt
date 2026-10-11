package app.rasoi.core.data.network

import app.rasoi.core.contract.ContractJson
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

/** Builds the shared HTTP client; tests pass a `MockEngine`. */
object HttpClientFactory {
    private const val REQUEST_TIMEOUT_MS = 30_000L
    private const val CONNECT_TIMEOUT_MS = 10_000L

    fun create(engine: HttpClientEngine): HttpClient = HttpClient(engine) { configure() }

    fun create(): HttpClient = HttpClient(platformEngine()) { configure() }

    private fun HttpClientConfig<*>.configure() {
        expectSuccess = false
        install(ContentNegotiation) { json(ContractJson) }
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
        }
    }
}

internal expect fun platformEngine(): HttpClientEngine
