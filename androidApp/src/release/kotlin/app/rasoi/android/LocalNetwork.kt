package app.rasoi.android

import androidx.activity.ComponentActivity

/** Release builds talk to a public HTTPS server, so they never need the local network permission. */
@Suppress("UnusedParameter")
internal fun requestLocalNetworkAccess(activity: ComponentActivity) = Unit
