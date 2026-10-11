package app.rasoi.feature.ledger

sealed interface LedgerEffect {
    /** Open this `upi://pay` link in the user's UPI app. */
    data class OpenUpi(val link: String) : LedgerEffect
}
