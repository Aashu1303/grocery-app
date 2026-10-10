package app.rasoi.core.model

/** Typed failure carried by [AppResult]; no exception crosses a layer boundary. */
sealed interface AppError {
    data object Offline : AppError

    data object Timeout : AppError

    data object Unauthenticated : AppError

    data object Forbidden : AppError

    data class NotFound(val resource: String) : AppError

    data class Conflict(val reason: String) : AppError

    data class Validation(val violations: List<FieldViolation>) : AppError

    data class Server(val status: Int) : AppError

    data class Unexpected(val message: String) : AppError

    data class FieldViolation(val field: String, val code: String)
}
