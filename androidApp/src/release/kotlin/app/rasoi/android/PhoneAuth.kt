package app.rasoi.android

import android.app.Application
import app.rasoi.core.domain.auth.PhoneAuthProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

/**
 * Release builds sign in with Firebase phone OTP (ADR 0024). FirebaseApp is auto-initialized
 * by the google-services plugin from google-services.json; this function just wires FirebaseAuth
 * into the provider. Returns UnconfiguredPhoneAuthProvider if Firebase was not initialized.
 */
internal fun phoneAuthProvider(application: Application): PhoneAuthProvider {
    val app = FirebaseApp.getApps(application).firstOrNull() ?: return UnconfiguredPhoneAuthProvider
    return FirebasePhoneAuthProvider(FirebaseAuth.getInstance(app), CurrentActivity(application))
}
