package app.rasoi.server.notifications

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.security.interfaces.RSAPublicKey
import java.util.Date
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Firebase Cloud Messaging over its HTTP v1 API. The service account signs a short-lived assertion that Google
 * exchanges for an access token, which is kept until shortly before it expires (ADR 0023).
 */
class FcmNotifier(
    private val http: HttpClient,
    private val account: ServiceAccount,
    private val clock: Clock,
    private val tokenUrl: String = "https://oauth2.googleapis.com/token",
    private val sendBase: String = "https://fcm.googleapis.com",
) : Notifier {
    private val lock = Mutex()
    private var accessToken: String? = null
    private var expiresAt: Instant = Instant.DISTANT_PAST

    override suspend fun send(tokens: List<String>, message: PushMessage): List<String> {
        val bearer = token() ?: return emptyList()
        return tokens.filter { deviceToken -> isUnregistered(deliver(bearer, deviceToken, message)) }
    }

    private suspend fun deliver(bearer: String, deviceToken: String, message: PushMessage): HttpStatusCode? = try {
        http.post("${sendBase.trimEnd('/')}/v1/projects/${account.projectId}/messages:send") {
            bearerAuth(bearer)
            setBody(TextContent(body(deviceToken, message), ContentType.Application.Json))
        }.status
    } catch (_: IOException) {
        null
    }

    private fun isUnregistered(status: HttpStatusCode?) = status == HttpStatusCode.NotFound

    private fun body(deviceToken: String, message: PushMessage): String = buildJsonObject {
        putJsonObject("message") {
            put("token", deviceToken)
            putJsonObject("notification") {
                put("title", message.title)
                put("body", message.body)
            }
            if (message.data.isNotEmpty()) {
                putJsonObject("data") {
                    message.data.forEach { (key, value) -> put(key, value) }
                }
            }
        }
    }.toString()

    private suspend fun token(): String? = lock.withLock {
        val now = clock.now()
        accessToken?.takeIf { now < expiresAt }?.let { return it }
        val response = try {
            http.post(tokenUrl) {
                setBody(
                    TextContent(
                        "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${assertion(now)}",
                        ContentType.Application.FormUrlEncoded,
                    ),
                )
            }
        } catch (_: IOException) {
            return null
        }
        if (!response.status.isSuccess()) return null
        val json = try {
            Json.parseToJsonElement(response.bodyAsText()).jsonObject
        } catch (_: IllegalArgumentException) {
            return null
        }
        val value = json["access_token"]?.jsonPrimitive?.content ?: return null
        val lifetime = json["expires_in"]?.jsonPrimitive?.long ?: 0L
        accessToken = value
        expiresAt = now + lifetime.coerceAtLeast(0L).seconds - EXPIRY_MARGIN
        value
    }

    private fun assertion(now: Instant): String = JWT.create()
        .withIssuer(account.clientEmail)
        .withClaim("scope", "https://www.googleapis.com/auth/firebase.messaging")
        .withAudience(tokenUrl)
        .withIssuedAt(Date(now.toEpochMilliseconds()))
        .withExpiresAt(Date((now + 1.hours).toEpochMilliseconds()))
        .sign(Algorithm.RSA256(null as RSAPublicKey?, account.privateKey))

    private companion object {
        /** Refresh a little early so a token never expires mid-request. */
        val EXPIRY_MARGIN = 1.minutes
    }
}
