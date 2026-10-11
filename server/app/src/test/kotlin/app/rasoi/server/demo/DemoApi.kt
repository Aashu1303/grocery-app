package app.rasoi.server.demo

import app.rasoi.core.contract.ApiHeaders
import app.rasoi.core.contract.HealthDto
import app.rasoi.core.contract.auth.CreateSessionRequestDto
import app.rasoi.core.contract.auth.MeDto
import app.rasoi.core.contract.auth.SessionDto
import app.rasoi.core.contract.household.CreateHouseholdRequestDto
import app.rasoi.core.contract.household.CreateInviteRequestDto
import app.rasoi.core.contract.household.CreateLocationRequestDto
import app.rasoi.core.contract.household.HouseholdDto
import app.rasoi.core.contract.household.InviteDto
import app.rasoi.core.contract.household.LocationDto
import app.rasoi.core.contract.household.LocationKindDto
import app.rasoi.core.contract.household.RoleDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.util.UUID
import kotlin.test.assertEquals

/**
 * The demo scenario's view of the server: typed calls over real HTTP that remember which endpoints were
 * exercised, so the scenario can prove it touches every endpoint (design-plan section 8).
 */
class DemoApi(private val client: HttpClient, private val baseUrl: String = "") {
    val exercised = mutableSetOf<String>()

    suspend fun health(): HealthDto = call("GET /health") { client.get("$baseUrl/health") }.body()

    suspend fun signIn(phone: String, name: String): SessionDto = call("POST /auth/session") {
        client.post("$baseUrl/auth/session") {
            contentType(ContentType.Application.Json)
            setBody(CreateSessionRequestDto("dev:$phone", name))
        }
    }.expect(HttpStatusCode.OK).body()

    suspend fun me(session: SessionDto): MeDto = call("GET /me") {
        client.get("$baseUrl/me") { authed(session) }
    }.expect(HttpStatusCode.OK).body()

    suspend fun createHousehold(owner: SessionDto, name: String): HouseholdDto = call("POST /households") {
        client.post("$baseUrl/households") {
            write(owner)
            setBody(CreateHouseholdRequestDto(name, "Asia/Kolkata"))
        }
    }.expect(HttpStatusCode.Created).body()

    suspend fun household(session: SessionDto, id: String): HttpResponse =
        call("GET /households/{id}") { client.get("$baseUrl/households/$id") { authed(session) } }

    suspend fun invite(session: SessionDto, householdId: String, role: RoleDto): HttpResponse =
        call("POST /households/{id}/invites") {
            client.post("$baseUrl/households/$householdId/invites") {
                write(session)
                setBody(CreateInviteRequestDto(role))
            }
        }

    suspend fun accept(session: SessionDto, code: String): HttpResponse = call("POST /invites/{code}/accept") {
        client.post("$baseUrl/invites/$code/accept") {
            write(session)
            setBody("{}")
        }
    }

    suspend fun locations(session: SessionDto, householdId: String): HttpResponse =
        call("GET /households/{id}/locations") {
            client.get("$baseUrl/households/$householdId/locations") { authed(session) }
        }

    suspend fun addLocation(
        session: SessionDto,
        householdId: String,
        name: String,
        kind: LocationKindDto,
    ): HttpResponse = call("POST /households/{id}/locations") {
        client.post("$baseUrl/households/$householdId/locations") {
            write(session)
            setBody(CreateLocationRequestDto(name, kind))
        }
    }

    suspend fun createdInvite(owner: SessionDto, householdId: String, role: RoleDto): InviteDto =
        invite(owner, householdId, role).expect(HttpStatusCode.Created).body()

    suspend fun locationList(session: SessionDto, householdId: String): List<LocationDto> =
        locations(session, householdId).expect(HttpStatusCode.OK).body()

    private suspend fun call(endpoint: String, request: suspend () -> HttpResponse): HttpResponse {
        exercised += endpoint
        return request()
    }

    private fun HttpRequestBuilder.authed(session: SessionDto) = bearerAuth(session.token)

    private fun HttpRequestBuilder.write(session: SessionDto) {
        authed(session)
        contentType(ContentType.Application.Json)
        header(ApiHeaders.IDEMPOTENCY_KEY, UUID.randomUUID().toString())
    }
}

fun HttpResponse.expect(status: HttpStatusCode): HttpResponse = also {
    assertEquals(status, it.status, "${call.request.url}")
}
