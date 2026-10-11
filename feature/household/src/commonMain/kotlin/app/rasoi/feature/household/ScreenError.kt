package app.rasoi.feature.household

import app.rasoi.core.model.AppError

/** Errors shown as a message rather than next to a field. */
enum class ScreenError {
    OFFLINE,
    FORBIDDEN,
    INVITE_USED,
    INVITE_EXPIRED,
    INVITE_NOT_FOUND,
    ALREADY_MEMBER,
    DUPLICATE,
    GENERIC,
}

internal fun AppError.screenError(): ScreenError = when (this) {
    AppError.Offline, AppError.Timeout -> ScreenError.OFFLINE
    AppError.Forbidden -> ScreenError.FORBIDDEN
    is AppError.NotFound -> if (resource == "invite") ScreenError.INVITE_NOT_FOUND else ScreenError.GENERIC
    is AppError.Conflict -> when (reason) {
        "invite_used" -> ScreenError.INVITE_USED
        "invite_expired" -> ScreenError.INVITE_EXPIRED
        "already_member" -> ScreenError.ALREADY_MEMBER
        "location_exists" -> ScreenError.DUPLICATE
        else -> ScreenError.GENERIC
    }
    else -> ScreenError.GENERIC
}
