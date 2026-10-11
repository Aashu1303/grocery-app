package app.rasoi.feature.ledger.upi

import androidx.compose.runtime.Composable

/** Opens a `upi://pay` link in the user's UPI app; false when no app could handle it. */
fun interface UpiLauncher {
    fun open(link: String): Boolean
}

@Composable
expect fun rememberUpiLauncher(): UpiLauncher
