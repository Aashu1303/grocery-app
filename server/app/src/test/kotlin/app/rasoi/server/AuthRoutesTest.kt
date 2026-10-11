package app.rasoi.server

import app.rasoi.core.contract.ErrorDto
import app.rasoi.core.contract.auth.CreateSessionRequestDto
import app.rasoi.core.contract.auth.MeDto
import app.rasoi.server.support.authed
import app.rasoi.server.support.rasoiTest
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Duration.Companion.days

class AuthRoutesTest {
    @Test
    fun firstSignInCreatesTheUserAndLaterSignInsReuseIt() = rasoiTest {
        val first = signIn("+919812345678", "Meera")
        val second = signIn("+919812345678", "Ignored")
        assertEquals(first.user, second.user)
        assertEquals("Meera", second.user.displayName)
        assertNotEquals(first.token, second.token)
    }

    @Test
    fun firstSignInWithoutDisplayNameIsAValidationError() = rasoiTest {
        val response = client.post("/auth/session") {
            contentType(ContentType.Application.Json)
            setBody(CreateSessionRequestDto(idToken = "dev:+919812345678"))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("displayName", response.body<ErrorDto>().fields.single().field)
    }

    @Test
    fun invalidIdTokenIsRejected() = rasoiTest {
        val response = client.post("/auth/session") {
            contentType(ContentType.Application.Json)
            setBody(CreateSessionRequestDto(idToken = "forged", displayName = "Eve"))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun protectedRoutesNeedAValidUnexpiredSession() = rasoiTest {
        assertEquals(HttpStatusCode.Unauthorized, client.get("/me").status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/me") { bearerAuth("not-a-session") }.status)

        val session = signIn("+919812345678", "Meera")
        val me = client.get("/me") { authed(session, idempotencyKey = null) }
        assertEquals(HttpStatusCode.OK, me.status)
        assertEquals("Meera", me.body<MeDto>().user.displayName)

        clock.advance(31.days)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/me") { authed(session, idempotencyKey = null) }.status)
    }
}
