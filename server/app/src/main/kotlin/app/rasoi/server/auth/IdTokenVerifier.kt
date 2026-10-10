package app.rasoi.server.auth

import app.rasoi.core.model.AppResult

/** Verifies an identity-provider ID token; failures are `AppError.Unauthenticated`. */
fun interface IdTokenVerifier {
    suspend fun verify(idToken: String): AppResult<VerifiedIdentity>
}
