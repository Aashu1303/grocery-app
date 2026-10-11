package app.rasoi.server.parsing

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult

internal object ParseFailures {
    fun unsupported(): AppResult<Nothing> =
        AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("document", "unsupported_format"))))

    fun unreadable(what: String): AppResult<Nothing> =
        AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("document", "unreadable_$what"))))
}
