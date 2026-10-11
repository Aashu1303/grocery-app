package app.rasoi.feature.ledger.upi

import androidx.compose.runtime.Composable

/** Desktop is only used for screenshots and tests, so there is no UPI app to open. */
@Composable
actual fun rememberUpiLauncher(): UpiLauncher = UpiLauncher { false }
