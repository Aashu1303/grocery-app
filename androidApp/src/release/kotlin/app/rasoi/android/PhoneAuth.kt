package app.rasoi.android

import android.app.Application
import app.rasoi.core.domain.auth.PhoneAuthProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth

/**
 * Release builds sign in with Firebase phone OTP (ADR 0024). The project's identifiers come from Gradle properties at
 * build time, never from the repository; without them the provider answers every request with an error.
 */
internal fun phoneAuthProvider(application: Application): PhoneAuthProvider {
    val configured = listOf(BuildConfig.FIREBASE_APP_ID, BuildConfig.FIREBASE_API_KEY, BuildConfig.FIREBASE_PROJECT_ID)
        .all { it.isNotBlank() }
    if (!configured) return UnconfiguredPhoneAuthProvider
    val options = FirebaseOptions.Builder()
        .setApplicationId(BuildConfig.FIREBASE_APP_ID)
        .setApiKey(BuildConfig.FIREBASE_API_KEY)
        .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
        .build()
    val app = FirebaseApp.initializeApp(application, options)
    return FirebasePhoneAuthProvider(FirebaseAuth.getInstance(app), CurrentActivity(application))
}
