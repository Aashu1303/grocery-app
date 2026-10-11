package app.rasoi.core.data

import app.rasoi.core.data.network.ApiConfig
import app.rasoi.core.data.network.HttpClientFactory
import app.rasoi.core.data.network.RasoiApi
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

/** A scripted server: each request is recorded and answered by [handler]. */
class MockServer(private val handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) {
    val requests = mutableListOf<HttpRequestData>()
    var unauthenticatedCalls = 0
    var token: String? = "session-token"
    private var keys = 0

    val api = RasoiApi(
        client = HttpClientFactory.create(
            MockEngine { request ->
                requests += request
                handler(request)
            },
        ),
        config = ApiConfig("https://api.rasoi.test/"),
        token = { token },
        onUnauthenticated = { unauthenticatedCalls++ },
        newIdempotencyKey = { "key-${++keys}" },
    )
}

fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK): HttpResponseData =
    respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
