package app.rasoi.server.routes

import app.rasoi.core.contract.devices.RegisterDeviceRequestDto
import app.rasoi.core.contract.kitchen.DishSuggestionsDto
import app.rasoi.core.contract.kitchen.UseFirstEntryDto
import app.rasoi.core.model.AppError
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.map
import app.rasoi.server.http.Idempotency
import app.rasoi.server.http.respondError
import app.rasoi.server.http.respondResult
import app.rasoi.server.http.toDto
import app.rasoi.server.http.toJsonResponse
import app.rasoi.server.persistence.parseUuid
import app.rasoi.server.service.DeviceService
import app.rasoi.server.service.KitchenService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/** Use-first list, dish suggestions and push-token registration (design-plan 6.6, ADR 0023). */
internal fun Route.kitchenRoutes(kitchen: KitchenService, devices: DeviceService, idempotency: Idempotency) {
    get("/households/{id}/kitchen/use-first") {
        val householdId = call.householdId() ?: return@get call.respondError(AppError.NotFound("household"))
        call.respondResult(
            kitchen.useFirst(call.userId(), householdId).map { list -> list.map { it.toDto() } },
            ListSerializer(UseFirstEntryDto.serializer()),
        )
    }

    post("/households/{id}/kitchen/suggestions") {
        val householdId = call.householdId() ?: return@post call.respondError(AppError.NotFound("household"))
        idempotency.handle(call, call.userId(), Unit.serializer()) {
            kitchen.suggestions(call.userId(), householdId)
                .map { dishes -> DishSuggestionsDto(dishes.map { it.toDto() }) }
                .toJsonResponse(DishSuggestionsDto.serializer(), HttpStatusCode.OK)
        }
    }

    post("/devices") {
        idempotency.handle(call, call.userId(), RegisterDeviceRequestDto.serializer()) { request ->
            devices.register(call.userId(), request.token, request.platform.name)
                .map { request.copy(token = request.token.trim()) }
                .toJsonResponse(RegisterDeviceRequestDto.serializer(), HttpStatusCode.OK)
        }
    }
}

private fun ApplicationCall.householdId(): HouseholdId? =
    parameters["id"]?.let(::parseUuid)?.let { HouseholdId(it.toString()) }
