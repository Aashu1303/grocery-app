package app.rasoi.server.parsing.fallback

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenAiChatClientTest {
    private val config = OpenAiChatClient.Config("https://llm.example/v1/", "secret-key", "small-model")
    private val requests = mutableListOf<Pair<HttpRequestData, String>>()

    private fun client(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        OpenAiChatClient(
            HttpClient(
                MockEngine { request ->
                    requests += request to request.body.toByteArray().decodeToString()
                    handler(request)
                },
            ),
            config,
        )

    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun postsAJsonModeChatRequestAndReturnsTheMessageContent() = runTest {
        val reply = """{"choices":[{"message":{"role":"assistant","content":"{\"total\":\"1.00\"}"}}]}"""
        val result = client { respond(reply, HttpStatusCode.OK, json) }.completeJson("system text", "user text")
        assertEquals(AppResult.Success("""{"total":"1.00"}"""), result)
        val (request, body) = requests.single()
        assertEquals("https://llm.example/v1/chat/completions", request.url.toString())
        assertEquals("Bearer secret-key", request.headers[HttpHeaders.Authorization])
        val sent = Json.parseToJsonElement(body).jsonObject
        assertEquals("small-model", sent.getValue("model").jsonPrimitive.content)
        assertEquals("json_object", sent.getValue("response_format").jsonObject.getValue("type").jsonPrimitive.content)
        val messages = sent.getValue("messages").jsonArray
            .map { it.jsonObject.getValue("content").jsonPrimitive.content }
        assertEquals(listOf("system text", "user text"), messages)
    }

    @Test
    fun mapsErrorStatusesMalformedBodiesAndNetworkFailures() = runTest {
        assertEquals(
            AppResult.Failure(AppError.Server(429)),
            client { respond("slow down", HttpStatusCode.TooManyRequests) }.completeJson("s", "u"),
        )
        val malformed = AppResult.Failure(AppError.Unexpected("llm_malformed_response"))
        listOf("[]", "{}", """{"choices":[]}""", "not json").forEach { body ->
            assertEquals(malformed, client { respond(body, HttpStatusCode.OK, json) }.completeJson("s", "u"), body)
        }
        assertEquals(
            AppResult.Failure(AppError.Unexpected("llm_unreachable")),
            client { throw IOException("down") }.completeJson("s", "u"),
        )
    }

    @Test
    fun neverPrintsTheApiKey() {
        assertEquals(false, "secret-key" in config.toString())
    }
}
