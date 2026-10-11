package app.rasoi.feature.bills

import app.rasoi.core.model.AppError

/** Failures shown as a message on the bill screens. */
enum class BillsError { OFFLINE, FORBIDDEN, NOT_FOUND, LOCKED, CHANGED, NOT_SPLIT, MISMATCH, GENERIC }

/** Problems shown next to a field of a form. */
enum class FieldError { REQUIRED, TOO_LONG, INVALID }

internal fun AppError.billsError(): BillsError = when (this) {
    AppError.Offline, AppError.Timeout -> BillsError.OFFLINE
    AppError.Forbidden -> BillsError.FORBIDDEN
    is AppError.NotFound -> BillsError.NOT_FOUND
    is AppError.Conflict -> conflictError(reason)
    is AppError.Validation -> violations.firstOrNull()?.let { violationError(it.field, it.code) } ?: BillsError.GENERIC
    else -> BillsError.GENERIC
}

private fun conflictError(reason: String): BillsError = when (reason) {
    "bill_not_editable" -> BillsError.LOCKED
    "bill_changed", "bill_not_pending", "bill_not_submittable" -> BillsError.CHANGED
    else -> BillsError.GENERIC
}

private fun violationError(field: String, code: String): BillsError = when {
    field.endsWith(".split") && code == "missing" -> BillsError.NOT_SPLIT
    field == "total" && code == "mismatch" -> BillsError.MISMATCH
    else -> BillsError.GENERIC
}

internal fun AppError.fieldError(field: String): FieldError? =
    (this as? AppError.Validation)?.violations?.firstOrNull { it.field == field }?.let {
        when (it.code) {
            "required", "blank" -> FieldError.REQUIRED
            "too_long" -> FieldError.TOO_LONG
            else -> FieldError.INVALID
        }
    }
