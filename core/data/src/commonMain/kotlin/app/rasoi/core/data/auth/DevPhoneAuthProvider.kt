package app.rasoi.core.data.auth

import app.rasoi.core.domain.auth.PhoneAuthProvider
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.PhoneNumber

/**
 * Sign-in for development builds against a server in `RASOI_AUTH_MODE=dev` (ADR 0016): no SMS is sent and the
 * code is always [DEV_CODE]. A server in Firebase mode rejects the resulting tokens.
 */
class DevPhoneAuthProvider : PhoneAuthProvider {
    override suspend fun requestCode(phone: PhoneNumber): AppResult<String> = AppResult.Success(phone.e164)

    override suspend fun confirm(verificationId: String, code: String): AppResult<String> = if (code == DEV_CODE) {
        AppResult.Success("dev:$verificationId")
    } else {
        AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("code", "incorrect"))))
    }

    companion object {
        const val DEV_CODE = "123456"
    }
}
