package app.rasoi.core.model

/** The signed-in user as the app knows them; the phone number stays on the server. */
data class Account(val id: UserId, val displayName: String)
