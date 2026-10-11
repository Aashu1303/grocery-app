package app.rasoi.feature.household

import app.rasoi.core.model.AppError

/** Inline form errors, keyed to string resources in the screen. */
enum class FieldError { INVALID, INCORRECT, REQUIRED, TOO_LONG }

internal fun AppError.fieldError(field: String): FieldError? =
    (this as? AppError.Validation)?.violations?.firstOrNull { it.field == field }?.let {
        when (it.code) {
            "incorrect" -> FieldError.INCORRECT
            "blank", "required" -> FieldError.REQUIRED
            "too_long" -> FieldError.TOO_LONG
            else -> FieldError.INVALID
        }
    }
