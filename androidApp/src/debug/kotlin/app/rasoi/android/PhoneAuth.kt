package app.rasoi.android

import android.app.Application
import app.rasoi.core.data.auth.DevPhoneAuthProvider
import app.rasoi.core.domain.auth.PhoneAuthProvider

/** Debug builds sign in without SMS against a dev-mode server (ADR 0016). Release builds have no such provider. */
@Suppress("UnusedParameter")
internal fun phoneAuthProvider(application: Application): PhoneAuthProvider = DevPhoneAuthProvider()
