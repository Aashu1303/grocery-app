package app.rasoi.server.routes

import app.rasoi.core.contract.auth.UpdateProfileRequestDto
import app.rasoi.core.contract.auth.UserDto
import app.rasoi.core.contract.ledger.LedgerDto
import app.rasoi.core.contract.ledger.TransferDto
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.PeriodMonth
import app.rasoi.core.model.SettlementId
import app.rasoi.core.model.map
import app.rasoi.server.http.Idempotency
import app.rasoi.server.http.respondError
import app.rasoi.server.http.respondResult
import app.rasoi.server.http.toDto
import app.rasoi.server.http.toJsonResponse
import app.rasoi.server.persistence.parseUuid
import app.rasoi.server.service.PeriodService
import app.rasoi.server.service.ProfileService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import kotlinx.serialization.builtins.serializer

/** Balances, month close, transfers and the caller's UPI address (design-plan 6.6, ADR 0022). */
internal fun Route.ledgerRoutes(periods: PeriodService, profile: ProfileService, idempotency: Idempotency) {
    patch("/me") {
        idempotency.handle(call, call.userId(), UpdateProfileRequestDto.serializer()) { request ->
            profile.updateUpi(call.userId(), request.upiVpa)
                .map { it.toDto() }
                .toJsonResponse(UserDto.serializer(), HttpStatusCode.OK)
        }
    }

    get("/households/{id}/ledger") {
        val householdId = call.householdId() ?: return@get call.respondError(AppError.NotFound("household"))
        when (val month = call.periodQuery()) {
            is AppResult.Failure -> call.respondError(month.error)
            is AppResult.Success -> call.respondResult(
                periods.ledger(call.userId(), householdId, month.value).map { it.toDto() },
                LedgerDto.serializer(),
            )
        }
    }

    post("/households/{id}/periods/{month}/close") {
        val householdId = call.householdId() ?: return@post call.respondError(AppError.NotFound("household"))
        val month = call.parameters["month"]?.let(PeriodMonth::parse)
            ?: return@post call.respondError(AppError.NotFound("period"))
        idempotency.handle(call, call.userId(), Unit.serializer()) {
            periods.close(call.userId(), householdId, month)
                .map { it.toDto() }
                .toJsonResponse(LedgerDto.serializer(), HttpStatusCode.OK)
        }
    }

    post("/settlements/{id}/paid") {
        val settlementId = call.parameters["id"]?.let(::parseUuid)?.let { SettlementId(it.toString()) }
            ?: return@post call.respondError(AppError.NotFound("settlement"))
        idempotency.handle(call, call.userId(), Unit.serializer()) {
            periods.markPaid(call.userId(), settlementId)
                .map { it.toDto() }
                .toJsonResponse(TransferDto.serializer(), HttpStatusCode.OK)
        }
    }
}

private fun ApplicationCall.householdId(): HouseholdId? =
    parameters["id"]?.let(::parseUuid)?.let { HouseholdId(it.toString()) }

/** `?period=YYYY-MM`; absent means the open period. */
private fun ApplicationCall.periodQuery(): AppResult<PeriodMonth?> {
    val raw = request.queryParameters["period"] ?: return AppResult.Success(null)
    return PeriodMonth.parse(raw)?.let { AppResult.Success(it) }
        ?: AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("period", "invalid"))))
}
