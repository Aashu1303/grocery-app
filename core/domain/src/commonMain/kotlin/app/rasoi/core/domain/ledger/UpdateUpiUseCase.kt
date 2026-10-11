package app.rasoi.core.domain.ledger

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult

/** Saves the caller's UPI address after checking its shape; a blank address clears it. */
class UpdateUpiUseCase(private val repository: LedgerRepository) {
    suspend operator fun invoke(vpa: String): AppResult<String?> {
        val address = vpa.trim().takeIf { it.isNotEmpty() }
        if (address != null && !UpiLink.isValidVpa(address)) {
            return AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("upiVpa", "invalid"))))
        }
        return repository.setUpiVpa(address)
    }
}
