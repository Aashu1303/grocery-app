package app.rasoi.core.data.network

import app.rasoi.core.contract.pantry.CreatePantryItemRequestDto
import app.rasoi.core.contract.pantry.PantryItemDto
import app.rasoi.core.contract.pantry.UpdatePantryItemRequestDto
import app.rasoi.core.model.AppResult
import io.ktor.http.HttpMethod
import kotlinx.serialization.builtins.ListSerializer

// The household pantry (design-plan 6.6, ADR 0020).
suspend fun RasoiApi.pantry(householdId: String, status: String): AppResult<List<PantryItemDto>> = send(
    HttpMethod.Get,
    "/households/$householdId/pantry?status=$status",
    ListSerializer(PantryItemDto.serializer()),
)

suspend fun RasoiApi.addPantryItem(householdId: String, request: CreatePantryItemRequestDto): AppResult<PantryItemDto> =
    write("/households/$householdId/pantry", PantryItemDto.serializer()) { jsonBody(request) }

suspend fun RasoiApi.updatePantryItem(
    householdId: String,
    itemId: String,
    request: UpdatePantryItemRequestDto,
): AppResult<PantryItemDto> =
    write("/households/$householdId/pantry/$itemId", PantryItemDto.serializer(), HttpMethod.Patch) {
        jsonBody(request)
    }
