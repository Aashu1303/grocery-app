package app.rasoi.core.domain.auth

import app.rasoi.core.model.Account

sealed interface AuthState {
    data object SignedOut : AuthState

    data class SignedIn(val account: Account) : AuthState
}
