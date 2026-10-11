package app.rasoi.core.domain.household

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult

class JoinHouseholdUseCase(private val repository: HouseholdRepository) {
    suspend operator fun invoke(rawCode: String): AppResult<HouseholdDetails> {
        val code = InviteCodeGenerator.normalise(rawCode)
            ?: return AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("code", "invalid"))))
        return repository.acceptInvite(code.value)
    }
}
