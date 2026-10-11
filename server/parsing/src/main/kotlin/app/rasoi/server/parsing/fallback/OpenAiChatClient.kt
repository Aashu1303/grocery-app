package app.rasoi.server.parsing.fallback

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException

/** An OpenAI-compatible `/chat/completions` endpoint in JSON mode. */
class OpenAiChatClient(private val http: HttpClient, private val config: Config) : ChatCompletionClient {
    data class Config(val baseUrl: String, val apiKey: String, val model: String) {
        override fun toString() = "Config(baseUrl=$baseUrl, model=$model)"
    }

    override suspend fun completeJson(system: String, user: String): AppResult<String> = try {
        val response = http.post("${config.baseUrl.trimEnd('/')}/chat/completions") {
            bearerAuth(config.apiKey)
            setBody(TextContent(requestBody(system, user).toString(), ContentType.Application.Json))
        }
        if (response.status.isSuccess()) {
            content(response.bodyAsText())
        } else {
            AppResult.Failure(AppError.Server(response.status.value))
        }
    } catch (_: HttpRequestTimeoutException) {
        AppResult.Failure(AppError.Timeout)
    } catch (_: IOException) {
        AppResult.Failure(AppError.Unexpected("llm_unreachable"))
    }

    private fun requestBody(system: String, user: String): JsonObject = buildJsonObject {
        put("model", config.model)
        put("temperature", 0)
        putJsonObject("response_format") { put("type", "json_object") }
        putJsonArray("messages") {
            addJsonObject {
                put("role", "system")
                put("content", system)
            }
            addJsonObject {
                put("role", "user")
                put("content", user)
            }
        }
    }

    private fun content(body: String): AppResult<String> = try {
        val message = Json.parseToJsonElement(body).jsonObject.getValue("choices").jsonArray.first()
            .jsonObject.getValue("message").jsonObject
        AppResult.Success(message.getValue("content").jsonPrimitive.content)
    } catch (_: IllegalArgumentException) { // includes SerializationException
        malformed()
    } catch (_: NoSuchElementException) {
        malformed()
    }

    private fun malformed(): AppResult<Nothing> = AppResult.Failure(AppError.Unexpected("llm_malformed_response"))
}
