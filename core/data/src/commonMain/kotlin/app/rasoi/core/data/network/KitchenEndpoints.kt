package app.rasoi.core.data.network

import app.rasoi.core.contract.kitchen.DishSuggestionsDto
import app.rasoi.core.contract.kitchen.UseFirstEntryDto
import app.rasoi.core.model.AppResult
import io.ktor.http.HttpMethod
import kotlinx.serialization.builtins.ListSerializer

// What to cook first and what to cook with it (design-plan 6.6, ADR 0023).
suspend fun RasoiApi.useFirst(householdId: String): AppResult<List<UseFirstEntryDto>> = send(
    HttpMethod.Get,
    "/households/$householdId/kitchen/use-first",
    ListSerializer(UseFirstEntryDto.serializer()),
)

suspend fun RasoiApi.suggestDishes(householdId: String): AppResult<DishSuggestionsDto> =
    write("/households/$householdId/kitchen/suggestions", DishSuggestionsDto.serializer()) {
        jsonBody(emptyMap<String, String>())
    }
