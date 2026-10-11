package app.rasoi.core.domain.auth

import app.rasoi.core.model.Account

sealed interface SignInOutcome {
    data class SignedIn(val account: Account) : SignInOutcome

    /** First sign-in: the server needs a display name before creating the user. */
    data class NeedsProfile(val idToken: String) : SignInOutcome
}
