package app.rasoi.core.domain.auth

import app.rasoi.core.model.AppResult
import app.rasoi.core.model.PhoneNumber

/** Phone OTP sign-in with an identity provider (Firebase in production, ADR 0016). */
interface PhoneAuthProvider {
    /** Sends a one-time code to [phone] and returns an opaque verification id. */
    suspend fun requestCode(phone: PhoneNumber): AppResult<String>

    /** Exchanges the [code] the user typed for an ID token the server can verify. */
    suspend fun confirm(verificationId: String, code: String): AppResult<String>
}
