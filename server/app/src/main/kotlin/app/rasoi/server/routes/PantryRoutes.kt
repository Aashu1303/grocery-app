package app.rasoi.server.routes

import app.rasoi.core.contract.pantry.CreatePantryItemRequestDto
import app.rasoi.core.contract.pantry.PantryItemDto
import app.rasoi.core.contract.pantry.UpdatePantryItemRequestDto
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.PantryItemId
import app.rasoi.core.model.PantryStatus
import app.rasoi.core.model.flatMap
import app.rasoi.core.model.map
import app.rasoi.server.http.Idempotency
import app.rasoi.server.http.respondError
import app.rasoi.server.http.respondResult
import app.rasoi.server.http.toDto
import app.rasoi.server.http.toJsonResponse
import app.rasoi.server.http.toModel
import app.rasoi.server.persistence.parseUuid
import app.rasoi.server.service.PantryService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import kotlinx.serialization.builtins.ListSerializer

/** Pantry endpoints (design-plan 6.6, ADR 0020). `?status=` filters; the default is items still in stock. */
internal fun Route.pantryRoutes(service: PantryService, idempotency: Idempotency) {
    get("/households/{id}/pantry") {
        val householdId = call.householdId() ?: return@get call.respondError(AppError.NotFound("household"))
        when (val status = call.statusFilter()) {
            is AppResult.Failure -> call.respondError(status.error)
            is AppResult.Success -> call.respondResult(
                service.list(call.userId(), householdId, status.value).map { items -> items.map { it.toDto() } },
                ListSerializer(PantryItemDto.serializer()),
            )
        }
    }

    post("/households/{id}/pantry") {
        val householdId = call.householdId() ?: return@post call.respondError(AppError.NotFound("household"))
        idempotency.handle(call, call.userId(), CreatePantryItemRequestDto.serializer()) { request ->
            readable(request.toModel())
                .flatMap { service.add(call.userId(), householdId, it) }
                .map { it.toDto() }
                .toJsonResponse(PantryItemDto.serializer(), HttpStatusCode.Created)
        }
    }

    patch("/households/{id}/pantry/{itemId}") {
        val householdId = call.householdId() ?: return@patch call.respondError(AppError.NotFound("household"))
        val itemId = call.parameters["itemId"]?.let(::parseUuid)?.let { PantryItemId(it.toString()) }
            ?: return@patch call.respondError(AppError.NotFound("pantry item"))
        idempotency.handle(call, call.userId(), UpdatePantryItemRequestDto.serializer()) { request ->
            readable(request.toModel())
                .flatMap { service.update(call.userId(), householdId, itemId, it) }
                .map { it.toDto() }
                .toJsonResponse(PantryItemDto.serializer(), HttpStatusCode.OK)
        }
    }
}

private fun <T : Any> readable(value: T?): AppResult<T> = value?.let { AppResult.Success(it) }
    ?: AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("body", "malformed"))))

private fun ApplicationCall.householdId(): HouseholdId? =
    parameters["id"]?.let(::parseUuid)?.let { HouseholdId(it.toString()) }

/** `?status=` is IN_STOCK (default), FINISHED, DISCARDED or ALL. */
private fun ApplicationCall.statusFilter(): AppResult<PantryStatus?> {
    val raw = request.queryParameters["status"] ?: return AppResult.Success(PantryStatus.IN_STOCK)
    if (raw.equals("all", ignoreCase = true)) return AppResult.Success(null)
    val status = PantryStatus.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
    return status?.let { AppResult.Success(it) }
        ?: AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("status", "invalid"))))
}
