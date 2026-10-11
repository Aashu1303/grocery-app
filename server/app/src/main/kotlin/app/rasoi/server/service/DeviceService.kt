package app.rasoi.server.service

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.UserId
import app.rasoi.server.persistence.DeviceStore
import kotlin.time.Clock

class DeviceService(private val devices: DeviceStore, private val clock: Clock) {
    /** Registers [token] for [userId]; registering the same token again is harmless. */
    suspend fun register(userId: UserId, token: String, platform: String): AppResult<Unit> {
        val cleaned = token.trim()
        if (cleaned.isEmpty() || cleaned.length > MAX_TOKEN_LENGTH) {
            return AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("token", "invalid"))))
        }
        devices.register(userId, cleaned, platform, clock.now())
        return AppResult.Success(Unit)
    }

    private companion object {
        const val MAX_TOKEN_LENGTH = 4096
    }
}
