package app.rasoi.android

import app.rasoi.core.domain.auth.PhoneAuthProvider
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.PhoneNumber

/** Stands in when a release was built without Firebase settings, so sign-in fails visibly instead of crashing. */
internal object UnconfiguredPhoneAuthProvider : PhoneAuthProvider {
    private val failure = AppResult.Failure(AppError.Unavailable("firebase_not_configured"))

    override suspend fun requestCode(phone: PhoneNumber): AppResult<String> = failure

    override suspend fun confirm(verificationId: String, code: String): AppResult<String> = failure
}
