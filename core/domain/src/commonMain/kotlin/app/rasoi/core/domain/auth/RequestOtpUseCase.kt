package app.rasoi.core.domain.auth

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.PhoneNumber
import app.rasoi.core.model.map

class RequestOtpUseCase(private val provider: PhoneAuthProvider) {
    suspend operator fun invoke(rawPhone: String): AppResult<OtpRequest> {
        val phone = PhoneNumber.parse(rawPhone)
            ?: return AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("phone", "invalid"))))
        return provider.requestCode(phone).map { OtpRequest(phone, it) }
    }
}
