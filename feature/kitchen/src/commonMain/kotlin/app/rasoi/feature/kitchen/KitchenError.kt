package app.rasoi.feature.kitchen

import app.rasoi.core.model.AppError

/** Failures shown as a message on the kitchen screen. */
enum class KitchenError { OFFLINE, FORBIDDEN, IDEAS_FAILED, GENERIC }

internal fun AppError.kitchenError(): KitchenError = when (this) {
    AppError.Offline, AppError.Timeout -> KitchenError.OFFLINE
    AppError.Forbidden -> KitchenError.FORBIDDEN
    is AppError.Unavailable -> KitchenError.IDEAS_FAILED
    else -> KitchenError.GENERIC
}
