package app.rasoi.server.http

import app.rasoi.core.contract.ErrorDto
import app.rasoi.core.contract.FieldErrorDto
import app.rasoi.core.model.AppError
import io.ktor.http.HttpStatusCode

/** The HTTP status and body for a typed failure; messages never contain user data. */
internal fun AppError.toHttp(): Pair<HttpStatusCode, ErrorDto> = when (this) {
    is AppError.Validation -> HttpStatusCode.BadRequest to ErrorDto(
        code = "validation_failed",
        message = "The request has invalid fields",
        fields = violations.map { FieldErrorDto(it.field, it.code) },
    )
    AppError.Unauthenticated -> HttpStatusCode.Unauthorized to ErrorDto("unauthenticated", "Sign in again")
    AppError.Forbidden -> HttpStatusCode.Forbidden to ErrorDto("forbidden", "Your role does not allow this")
    is AppError.NotFound -> HttpStatusCode.NotFound to ErrorDto("not_found", "No such $resource")
    is AppError.Conflict -> HttpStatusCode.Conflict to ErrorDto(reason, "The request conflicts with current state")
    AppError.Offline, AppError.Timeout, is AppError.Server, is AppError.Unexpected ->
        HttpStatusCode.InternalServerError to ErrorDto("internal", "Unexpected error")
}
