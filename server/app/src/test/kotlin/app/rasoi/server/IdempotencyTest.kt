package app.rasoi.server

import app.rasoi.core.contract.ErrorDto
import app.rasoi.core.contract.auth.MeDto
import app.rasoi.core.contract.household.CreateHouseholdRequestDto
import app.rasoi.core.contract.household.HouseholdDto
import app.rasoi.server.support.authed
import app.rasoi.server.support.rasoiTest
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals

class IdempotencyTest {
    @Test
    fun retryWithTheSameKeyReplaysTheFirstResponse() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val first = client.post("/households") {
            authed(owner, idempotencyKey = "create-household-1")
            setBody(CreateHouseholdRequestDto("Flat 4B", "Asia/Kolkata"))
        }
        val retry = client.post("/households") {
            authed(owner, idempotencyKey = "create-household-1")
            setBody(CreateHouseholdRequestDto("Flat 4B", "Asia/Kolkata"))
        }
        assertEquals(HttpStatusCode.Created, retry.status)
        assertEquals(first.body<HouseholdDto>().id, retry.body<HouseholdDto>().id)
        assertEquals(1, client.get("/me") { authed(owner, null) }.body<MeDto>().households.size)
    }

    @Test
    fun reusingAKeyForADifferentRequestIsRejected() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        client.post("/households") {
            authed(owner, idempotencyKey = "same-key-123")
            setBody(CreateHouseholdRequestDto("Flat 4B", "Asia/Kolkata"))
        }
        val reused = client.post("/households") {
            authed(owner, idempotencyKey = "same-key-123")
            setBody(CreateHouseholdRequestDto("Flat 9C", "Asia/Kolkata"))
        }
        assertEquals(HttpStatusCode.Conflict, reused.status)
        assertEquals("idempotency_key_reused", reused.body<ErrorDto>().code)
    }

    @Test
    fun mutatingRequestsWithoutAKeyAreRejected() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val response = client.post("/households") {
            authed(owner, idempotencyKey = null)
            setBody(CreateHouseholdRequestDto("Flat 4B", "Asia/Kolkata"))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun malformedBodiesAreValidationErrors() = rasoiTest {
        val owner = signIn("+919812345678", "Meera")
        val response = client.post("/households") {
            authed(owner)
            setBody("""{"name": 3""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("body", response.body<ErrorDto>().fields.single().field)
    }
}
