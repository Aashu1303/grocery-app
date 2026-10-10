package app.rasoi.server.auth

import app.rasoi.core.model.UserId

/** The authenticated caller of a request. */
data class UserPrincipal(val userId: UserId)
