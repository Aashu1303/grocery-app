package app.rasoi.server.notifications

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class FcmNotifierTest {
    private val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val account =
        ServiceAccount("rasoi-test", "push@rasoi-test.iam.gserviceaccount.com", pair.private as RSAPrivateKey)

    private class TestClock(var now: Instant = Instant.parse("2026-10-11T10:00:00Z")) : Clock {
        override fun now() = now
    }

    private class Recorder {
        val requests = mutableListOf<Pair<String, String>>()
        var tokenCalls = 0
    }

    private fun client(recorder: Recorder, sendStatus: (String) -> HttpStatusCode = { HttpStatusCode.OK }) = HttpClient(
        MockEngine { request ->
            val body = request.body.toByteArray().decodeToString()
            recorder.requests += request.url.toString() to body
            when {
                request.url.host == "oauth2.googleapis.com" -> {
                    recorder.tokenCalls++
                    respond(
                        """{"access_token":"ya29.token","expires_in":3600}""",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
                else -> respond("{}", sendStatus(body), headersOf(HttpHeaders.ContentType, "application/json"))
            }
        },
    )

    @Test
    fun exchangesASignedAssertionForAnAccessTokenAndSendsEachMessage() = runTest {
        val recorder = Recorder()
        val clock = TestClock()
        val invalid = FcmNotifier(client(recorder), account, clock)
            .send(listOf("tok-1", "tok-2"), PushMessage("Use soon", "Milk expires today", mapOf("kind" to "expiry")))

        assertEquals(emptyList(), invalid)
        val (exchange, first, second) = recorder.requests
        val assertion = exchange.second.substringAfter("assertion=")
        val verified = JWT.decode(assertion)
        Algorithm.RSA256(pair.public as RSAPublicKey, null).verify(verified)
        assertEquals(account.clientEmail, verified.issuer)
        assertEquals("https://www.googleapis.com/auth/firebase.messaging", verified.getClaim("scope").asString())
        assertTrue(exchange.second.startsWith("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer"))

        assertEquals("https://fcm.googleapis.com/v1/projects/rasoi-test/messages:send", first.first)
        val message = Json.parseToJsonElement(first.second).jsonObject.getValue("message").jsonObject
        assertEquals("tok-1", message.getValue("token").jsonPrimitive.content)
        assertEquals("Use soon", message.getValue("notification").jsonObject.getValue("title").jsonPrimitive.content)
        assertEquals("expiry", message.getValue("data").jsonObject.getValue("kind").jsonPrimitive.content)
        assertTrue("tok-2" in second.second)
    }

    @Test
    fun theAccessTokenIsReusedUntilItIsAboutToExpire() = runTest {
        val recorder = Recorder()
        val clock = TestClock()
        val notifier = FcmNotifier(client(recorder), account, clock)
        notifier.send(listOf("a"), PushMessage("t", "b"))
        clock.now += 30.minutes
        notifier.send(listOf("a"), PushMessage("t", "b"))
        assertEquals(1, recorder.tokenCalls)
        clock.now += 40.minutes
        notifier.send(listOf("a"), PushMessage("t", "b"))
        assertEquals(2, recorder.tokenCalls)
    }

    @Test
    fun tokensTheProviderNoLongerKnowsAreReturnedForRemoval() = runTest {
        val recorder = Recorder()
        val notifier = FcmNotifier(
            client(recorder) { body ->
                if ("gone" in
                    body
                ) {
                    HttpStatusCode.NotFound
                } else {
                    HttpStatusCode.OK
                }
            },
            account,
            TestClock(),
        )
        assertEquals(listOf("gone"), notifier.send(listOf("fine", "gone"), PushMessage("t", "b")))
    }

    @Test
    fun aFailedTokenExchangeSendsNothingAndDropsNoTokens() = runTest {
        val engine = MockEngine { respond("{}", HttpStatusCode.Unauthorized) }
        assertEquals(
            emptyList(),
            FcmNotifier(HttpClient(engine), account, TestClock()).send(listOf("a"), PushMessage("t", "b")),
        )
    }

    @Test
    fun parsesAServiceAccountFileAndRejectsBrokenOnes() {
        val pem = "-----BEGIN PRIVATE KEY-----\n" +
            Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(pair.private.encoded) +
            "\n-----END PRIVATE KEY-----\n"
        val json = Json.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(),
            kotlinx.serialization.json.JsonObject(
                mapOf(
                    "project_id" to kotlinx.serialization.json.JsonPrimitive("p"),
                    "client_email" to kotlinx.serialization.json.JsonPrimitive("e@p"),
                    "private_key" to kotlinx.serialization.json.JsonPrimitive(pem),
                ),
            ),
        )
        assertEquals("p", ServiceAccount.parse(json)?.projectId)
        assertNull(ServiceAccount.parse("{}"))
        assertNull(ServiceAccount.parse("not json"))
        assertNull(ServiceAccount.parse(json.replace("PRIVATE KEY", "PRIVATE KEX").replace("MIIE", "!!!!")))
    }

    @Test
    fun theNoopNotifierKeepsEveryToken() = runTest {
        assertEquals(emptyList(), NoopNotifier.send(listOf("a"), PushMessage("t", "b")))
    }
}
