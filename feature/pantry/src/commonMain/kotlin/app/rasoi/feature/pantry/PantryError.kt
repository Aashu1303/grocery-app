package app.rasoi.feature.pantry

import app.rasoi.core.model.AppError

/** Failures shown as a message on the pantry screen. */
enum class PantryError { OFFLINE, FORBIDDEN, NOT_FOUND, GENERIC }

/** Problems shown next to a field of the add form. */
enum class PantryFieldError { REQUIRED, TOO_LONG, INVALID }

internal fun AppError.pantryError(): PantryError = when (this) {
    AppError.Offline, AppError.Timeout -> PantryError.OFFLINE
    AppError.Forbidden -> PantryError.FORBIDDEN
    is AppError.NotFound -> PantryError.NOT_FOUND
    else -> PantryError.GENERIC
}

internal fun AppError.fieldError(field: String): PantryFieldError? =
    (this as? AppError.Validation)?.violations?.firstOrNull { it.field == field }?.let {
        when (it.code) {
            "required", "blank" -> PantryFieldError.REQUIRED
            "too_long" -> PantryFieldError.TOO_LONG
            else -> PantryFieldError.INVALID
        }
    }
