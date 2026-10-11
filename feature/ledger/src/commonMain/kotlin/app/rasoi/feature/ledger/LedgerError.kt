package app.rasoi.feature.ledger

import app.rasoi.core.model.AppError

/** Failures shown as a message on the balances screen. */
enum class LedgerError { OFFLINE, FORBIDDEN, NOT_FOUND, CHANGED, NO_UPI_APP, GENERIC }

internal fun AppError.ledgerError(): LedgerError = when (this) {
    AppError.Offline, AppError.Timeout -> LedgerError.OFFLINE
    AppError.Forbidden -> LedgerError.FORBIDDEN
    is AppError.NotFound -> LedgerError.NOT_FOUND
    is AppError.Conflict -> if (reason.startsWith("period_")) LedgerError.CHANGED else LedgerError.GENERIC
    else -> LedgerError.GENERIC
}
