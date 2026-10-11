package app.rasoi.core.data.network

import app.rasoi.core.contract.auth.UpdateProfileRequestDto
import app.rasoi.core.contract.auth.UserDto
import app.rasoi.core.contract.ledger.LedgerDto
import app.rasoi.core.contract.ledger.TransferDto
import app.rasoi.core.model.AppResult
import io.ktor.http.HttpMethod

// Balances, month close, settlements and the caller's UPI address (design-plan 6.6, ADR 0022).
suspend fun RasoiApi.ledger(householdId: String, month: String?): AppResult<LedgerDto> = send(
    HttpMethod.Get,
    "/households/$householdId/ledger" + month?.let { "?period=$it" }.orEmpty(),
    LedgerDto.serializer(),
)

suspend fun RasoiApi.closeMonth(householdId: String, month: String): AppResult<LedgerDto> =
    write("/households/$householdId/periods/$month/close", LedgerDto.serializer()) {
        jsonBody(emptyMap<String, String>())
    }

suspend fun RasoiApi.markPaid(settlementId: String): AppResult<TransferDto> =
    write("/settlements/$settlementId/paid", TransferDto.serializer()) { jsonBody(emptyMap<String, String>()) }

suspend fun RasoiApi.updateProfile(request: UpdateProfileRequestDto): AppResult<UserDto> =
    write("/me", UserDto.serializer(), HttpMethod.Patch) { jsonBody(request) }
