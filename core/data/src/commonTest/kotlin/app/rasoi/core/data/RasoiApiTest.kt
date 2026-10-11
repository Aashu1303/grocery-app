package app.rasoi.core.data

import app.rasoi.core.contract.ApiHeaders
import app.rasoi.core.contract.household.CreateHouseholdRequestDto
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class RasoiApiTest {
    private val householdJson =
        """{"id":"h1","name":"Flat 4B","timeZone":"Asia/Kolkata","members":[],"locations":[]}"""

    @Test
    fun authenticatedReadsSendTheBearerToken() = runTest {
        val server = MockServer { json("""{"user":{"id":"u1","displayName":"Meera"},"households":[]}""") }
        assertIs<AppResult.Success<*>>(server.api.me())
        val request = server.requests.single()
        assertEquals("https://api.rasoi.test/me", request.url.toString())
        assertEquals("Bearer session-token", request.headers[HttpHeaders.Authorization])
        assertNull(request.headers[ApiHeaders.IDEMPOTENCY_KEY])
    }

    @Test
    fun writesCarryAnIdempotencyKey() = runTest {
        val server = MockServer { json(householdJson, HttpStatusCode.Created) }
        server.api.createHousehold(CreateHouseholdRequestDto("Flat 4B", "Asia/Kolkata"))
        val request = server.requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("key-1", request.headers[ApiHeaders.IDEMPOTENCY_KEY])
    }

    @Test
    fun networkFailuresRetryWritesWithTheSameKey() = runTest {
        var calls = 0
        val server = MockServer {
            if (++calls < 3) throw IOException("connection reset") else json(householdJson, HttpStatusCode.Created)
        }
        assertIs<AppResult.Success<*>>(server.api.createHousehold(CreateHouseholdRequestDto("Flat", "Asia/Kolkata")))
        assertEquals(listOf("key-1", "key-1", "key-1"), server.requests.map { it.headers[ApiHeaders.IDEMPOTENCY_KEY] })
    }

    @Test
    fun persistentOutageReportsOfflineAfterRetries() = runTest {
        val server = MockServer { throw IOException("no route") }
        assertEquals(
            AppResult.Failure(AppError.Offline),
            server.api.createHousehold(CreateHouseholdRequestDto("Flat", "Asia/Kolkata")),
        )
        assertEquals(3, server.requests.size)
        assertEquals(AppResult.Failure(AppError.Offline), server.api.me())
    }

    @Test
    fun httpErrorsMapToTypedErrors() = runTest {
        val cases = mapOf(
            """{"code":"validation_failed","message":"x","fields":[{"field":"name","code":"blank"}]}""" to
                (HttpStatusCode.BadRequest to AppError.Validation(listOf(AppError.FieldViolation("name", "blank")))),
            """{"code":"forbidden","message":"x"}""" to (HttpStatusCode.Forbidden to AppError.Forbidden),
            """{"code":"not_found","message":"No such household"}""" to
                (HttpStatusCode.NotFound to AppError.NotFound("household")),
            """{"code":"invite_used","message":"x"}""" to (HttpStatusCode.Conflict to AppError.Conflict("invite_used")),
            "<html>bad gateway</html>" to (HttpStatusCode.BadGateway to AppError.Server(502)),
        )
        cases.forEach { (body, expected) ->
            val server = MockServer { json(body, expected.first) }
            assertEquals(AppResult.Failure(expected.second), server.api.household("h1"), body)
        }
    }

    @Test
    fun unauthorizedNotifiesTheSessionOwner() = runTest {
        val server = MockServer { json("""{"code":"unauthenticated","message":"x"}""", HttpStatusCode.Unauthorized) }
        assertEquals(AppResult.Failure(AppError.Unauthenticated), server.api.me())
        assertEquals(1, server.unauthenticatedCalls)
    }

    @Test
    fun malformedSuccessBodiesAreUnexpected() = runTest {
        val server = MockServer { json("""{"unexpected":true}""") }
        assertIs<AppError.Unexpected>((server.api.household("h1") as AppResult.Failure).error)
    }
}
