package app.rasoi.core.domain.auth

import app.rasoi.core.model.User

sealed interface AuthState {
    data object SignedOut : AuthState

    data class SignedIn(val user: User) : AuthState
}
