package app.rasoi.feature.invoiceimport

import app.rasoi.core.model.AppError

/** Failures shown as a message on the import and review screens. */
enum class BillError {
    OFFLINE,
    FORBIDDEN,
    NOT_FOUND,
    NOT_EDITABLE,
    DUPLICATE,
    NOT_A_PDF,
    ENCRYPTED,
    NO_TEXT,
    UNSUPPORTED,
    DIFFERENT_ORDERS,
    REPEATED,
    TOO_LARGE,
    TOO_MANY,
    GENERIC,
}

internal fun AppError.billError(): BillError = when (this) {
    AppError.Offline, AppError.Timeout -> BillError.OFFLINE
    AppError.Forbidden -> BillError.FORBIDDEN
    is AppError.NotFound -> BillError.NOT_FOUND
    is AppError.Conflict -> when (reason) {
        "duplicate_invoice" -> BillError.DUPLICATE
        "bill_not_editable" -> BillError.NOT_EDITABLE
        else -> BillError.GENERIC
    }
    is AppError.Validation -> violations.firstOrNull()?.code.let(::validationError)
    else -> BillError.GENERIC
}

private fun validationError(code: String?): BillError = when (code) {
    "not_a_pdf", "empty" -> BillError.NOT_A_PDF
    "encrypted" -> BillError.ENCRYPTED
    "no_text" -> BillError.NO_TEXT
    "different_orders" -> BillError.DIFFERENT_ORDERS
    "repeated_invoice" -> BillError.REPEATED
    "too_large" -> BillError.TOO_LARGE
    "count" -> BillError.TOO_MANY
    else -> if (code?.startsWith("unreadable") == true || code == "unsupported_format") {
        BillError.UNSUPPORTED
    } else {
        BillError.GENERIC
    }
}
