package app.rasoi.server.routes

import app.rasoi.core.contract.auth.MeDto
import app.rasoi.core.contract.household.CreateHouseholdRequestDto
import app.rasoi.core.contract.household.CreateInviteRequestDto
import app.rasoi.core.contract.household.CreateLocationRequestDto
import app.rasoi.core.contract.household.HouseholdDto
import app.rasoi.core.contract.household.InviteDto
import app.rasoi.core.contract.household.LocationDto
import app.rasoi.core.model.AppError
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.map
import app.rasoi.server.auth.UserPrincipal
import app.rasoi.server.http.Idempotency
import app.rasoi.server.http.respondError
import app.rasoi.server.http.respondResult
import app.rasoi.server.http.toDto
import app.rasoi.server.http.toJsonResponse
import app.rasoi.server.http.toMeDto
import app.rasoi.server.http.toModel
import app.rasoi.server.persistence.parseUuid
import app.rasoi.server.service.HouseholdService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/** Household, invite and location endpoints (design-plan 6.6). All require an authenticated session. */
internal fun Route.householdRoutes(service: HouseholdService, idempotency: Idempotency) {
    get("/me") { call.respondResult(service.me(call.userId()).map { it.toMeDto() }, MeDto.serializer()) }

    post("/households") {
        idempotency.handle(call, call.userId(), CreateHouseholdRequestDto.serializer()) { request ->
            service.create(call.userId(), request.name, request.timeZone)
                .map { it.toDto() }
                .toJsonResponse(HouseholdDto.serializer(), HttpStatusCode.Created)
        }
    }

    get("/households/{id}") {
        val householdId = call.householdId() ?: return@get call.respondError(AppError.NotFound("household"))
        call.respondResult(service.detail(call.userId(), householdId).map { it.toDto() }, HouseholdDto.serializer())
    }

    post("/households/{id}/invites") {
        val householdId = call.householdId() ?: return@post call.respondError(AppError.NotFound("household"))
        idempotency.handle(call, call.userId(), CreateInviteRequestDto.serializer()) { request ->
            service.createInvite(call.userId(), householdId, request.role.toModel())
                .map { it.toDto() }
                .toJsonResponse(InviteDto.serializer(), HttpStatusCode.Created)
        }
    }

    post("/invites/{code}/accept") {
        val code = call.parameters["code"].orEmpty()
        idempotency.handle(call, call.userId(), Unit.serializer()) {
            service.acceptInvite(call.userId(), code).map {
                it.toDto()
            }.toJsonResponse(HouseholdDto.serializer(), HttpStatusCode.OK)
        }
    }

    get("/households/{id}/locations") {
        val householdId = call.householdId() ?: return@get call.respondError(AppError.NotFound("household"))
        call.respondResult(
            service.locations(call.userId(), householdId).map { list -> list.map { it.toDto() } },
            ListSerializer(LocationDto.serializer()),
        )
    }

    post("/households/{id}/locations") {
        val householdId = call.householdId() ?: return@post call.respondError(AppError.NotFound("household"))
        idempotency.handle(call, call.userId(), CreateLocationRequestDto.serializer()) { request ->
            service.addLocation(call.userId(), householdId, request.name, request.kind.toModel())
                .map { it.toDto() }
                .toJsonResponse(LocationDto.serializer(), HttpStatusCode.Created)
        }
    }
}

internal fun ApplicationCall.userId() = requireNotNull(principal<UserPrincipal>()) {
    "Route needs authentication"
}.userId

private fun ApplicationCall.householdId(): HouseholdId? =
    parameters["id"]?.let(::parseUuid)?.let { HouseholdId(it.toString()) }
