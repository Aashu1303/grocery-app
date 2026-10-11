package app.rasoi.core.testing

import app.rasoi.core.domain.auth.PhoneAuthProvider
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.PhoneNumber

/** Accepts [validCode] for any phone and returns `token-<e164>`; failures can be forced per step. */
class FakePhoneAuthProvider(private val validCode: String = "123456") : PhoneAuthProvider {
    var requestFailure: AppError? = null
    val requested = mutableListOf<PhoneNumber>()

    override suspend fun requestCode(phone: PhoneNumber): AppResult<String> {
        requested += phone
        return requestFailure?.let { AppResult.Failure(it) } ?: AppResult.Success("verification-${phone.e164}")
    }

    override suspend fun confirm(verificationId: String, code: String): AppResult<String> = if (code == validCode) {
        AppResult.Success("token-" + verificationId.removePrefix("verification-"))
    } else {
        AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("code", "incorrect"))))
    }
}
