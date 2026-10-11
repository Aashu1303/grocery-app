package app.rasoi.server.service

import app.rasoi.core.domain.ledger.UpiLink
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.User
import app.rasoi.core.model.UserId
import app.rasoi.server.persistence.UserStore

/** The caller's own details; today that is the UPI address flatmates pay them on. */
class ProfileService(private val users: UserStore) {
    /** Sets the UPI address, or clears it when [vpa] is null or blank. */
    suspend fun updateUpi(userId: UserId, vpa: String?): AppResult<User> {
        val address = vpa?.trim()?.takeIf { it.isNotEmpty() }
        if (address != null && !UpiLink.isValidVpa(address)) {
            return AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("upiVpa", "invalid"))))
        }
        users.setUpiVpa(userId, address)
        return users.find(userId)?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.Unauthenticated)
    }
}
