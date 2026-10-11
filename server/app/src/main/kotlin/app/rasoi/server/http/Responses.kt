package app.rasoi.server.http

import app.rasoi.core.contract.ContractJson
import app.rasoi.core.contract.ErrorDto
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import kotlinx.serialization.KSerializer

internal fun <T> AppResult<T>.toJsonResponse(serializer: KSerializer<T>, successStatus: HttpStatusCode): JsonResponse =
    when (this) {
        is AppResult.Success -> JsonResponse(successStatus, ContractJson.encodeToString(serializer, value))
        is AppResult.Failure -> error.toHttp().let { (status, dto) ->
            JsonResponse(status, ContractJson.encodeToString(ErrorDto.serializer(), dto))
        }
    }

internal suspend fun ApplicationCall.respondJson(response: JsonResponse) =
    respondText(response.body, ContentType.Application.Json, response.status)

internal suspend fun <T : Any> ApplicationCall.respondResult(
    result: AppResult<T>,
    serializer: KSerializer<T>,
    successStatus: HttpStatusCode = HttpStatusCode.OK,
) = respondJson(result.toJsonResponse(serializer, successStatus))

internal suspend fun ApplicationCall.respondError(error: AppError) {
    val (status, dto) = error.toHttp()
    respond(status, dto)
}
