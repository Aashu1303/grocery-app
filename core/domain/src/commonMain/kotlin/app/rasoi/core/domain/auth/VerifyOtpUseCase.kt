package app.rasoi.core.domain.auth

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.flatMap

/** Confirms the one-time code and signs in; a first-time user comes back as [SignInOutcome.NeedsProfile]. */
class VerifyOtpUseCase(private val provider: PhoneAuthProvider, private val auth: AuthRepository) {
    suspend operator fun invoke(verificationId: String, code: String): AppResult<SignInOutcome> {
        val digits = code.filter { !it.isWhitespace() }
        if (digits.length != CODE_LENGTH || !digits.all { it.isDigit() }) {
            return AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("code", "invalid"))))
        }
        return withProviderTimeout { provider.confirm(verificationId, digits) }.flatMap { idToken ->
            when (val result = auth.signIn(idToken, displayName = null)) {
                is AppResult.Success -> AppResult.Success(SignInOutcome.SignedIn(result.value))
                is AppResult.Failure -> if (result.error.needsDisplayName()) {
                    AppResult.Success(SignInOutcome.NeedsProfile(idToken))
                } else {
                    result
                }
            }
        }
    }

    private fun AppError.needsDisplayName() =
        this is AppError.Validation && violations.any { it.field == "displayName" && it.code == "required" }

    private companion object {
        const val CODE_LENGTH = 6
    }
}
