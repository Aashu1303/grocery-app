package app.rasoi.server.auth

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.PhoneNumber

/**
 * Development and demo sign-in without Firebase: accepts tokens of the form `dev:<phone>`.
 * Only wired when `RASOI_AUTH_MODE=dev` (ADR 0015); never in production.
 */
class DevIdTokenVerifier : IdTokenVerifier {
    override suspend fun verify(idToken: String): AppResult<VerifiedIdentity> {
        val phone = idToken.takeIf { it.startsWith(PREFIX) }?.removePrefix(PREFIX)?.let(PhoneNumber::parse)
            ?: return AppResult.Failure(AppError.Unauthenticated)
        return AppResult.Success(VerifiedIdentity(firebaseUid = "dev-${phone.e164}", phone = phone))
    }

    companion object {
        const val PREFIX = "dev:"
    }
}
