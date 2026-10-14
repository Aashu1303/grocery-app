package app.rasoi.core.testing

import app.rasoi.core.domain.auth.PhoneAuthProvider
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.PhoneNumber
import kotlinx.coroutines.awaitCancellation

/**
 * Accepts [validCode] for any phone and returns `token-<e164>`; failures can be forced per step, and [silent] makes
 * every call wait forever, as a native SDK does when its callback never comes.
 */
class FakePhoneAuthProvider(private val validCode: String = "123456") : PhoneAuthProvider {
    var requestFailure: AppError? = null
    var silent = false
    val requested = mutableListOf<PhoneNumber>()

    override suspend fun requestCode(phone: PhoneNumber): AppResult<String> {
        requested += phone
        if (silent) awaitCancellation()
        return requestFailure?.let { AppResult.Failure(it) } ?: AppResult.Success("verification-${phone.e164}")
    }

    override suspend fun confirm(verificationId: String, code: String): AppResult<String> {
        if (silent) awaitCancellation()
        return if (code == validCode) {
            AppResult.Success("token-" + verificationId.removePrefix("verification-"))
        } else {
            AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("code", "incorrect"))))
        }
    }
}
