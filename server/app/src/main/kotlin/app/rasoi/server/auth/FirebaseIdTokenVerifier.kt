package app.rasoi.server.auth

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.PhoneNumber
import com.auth0.jwk.JwkException
import com.auth0.jwk.JwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.interfaces.RSAPublicKey
import kotlin.time.Clock
import kotlin.time.toJavaInstant

/**
 * Verifies Firebase Auth ID tokens as Firebase documents: RS256 signed by a Google key from [keys], issuer
 * `https://securetoken.google.com/<projectId>`, audience [projectId], unexpired, with a subject and a phone number.
 */
class FirebaseIdTokenVerifier(private val projectId: String, private val keys: JwkProvider, private val clock: Clock) :
    IdTokenVerifier {
    override suspend fun verify(idToken: String): AppResult<VerifiedIdentity> = withContext(Dispatchers.IO) {
        try {
            val keyId = JWT.decode(idToken).keyId ?: throw JWTVerificationException("missing kid")
            val publicKey = keys.get(keyId).publicKey as? RSAPublicKey ?: throw JWTVerificationException("not RSA")
            val token = verifier(Algorithm.RSA256(publicKey, null)).verify(idToken)
            val phone = PhoneNumber.parse(token.getClaim(PHONE_CLAIM).asString().orEmpty())
            val subject = token.subject
            if (phone == null || subject.isNullOrBlank()) {
                AppResult.Failure(AppError.Unauthenticated)
            } else {
                AppResult.Success(VerifiedIdentity(subject, phone))
            }
        } catch (_: JWTVerificationException) {
            AppResult.Failure(AppError.Unauthenticated)
        } catch (_: JwkException) {
            AppResult.Failure(AppError.Unauthenticated)
        }
    }

    private fun verifier(algorithm: Algorithm): JWTVerifier {
        val verification = JWT.require(algorithm)
            .withIssuer("$ISSUER_PREFIX$projectId")
            .withAudience(projectId)
            .withClaimPresence(PHONE_CLAIM)
            .acceptLeeway(LEEWAY_SECONDS) as JWTVerifier.BaseVerification
        val now = clock.now().toJavaInstant()
        return verification.build(java.time.Clock.fixed(now, java.time.ZoneOffset.UTC))
    }

    private companion object {
        const val ISSUER_PREFIX = "https://securetoken.google.com/"
        const val PHONE_CLAIM = "phone_number"
        const val LEEWAY_SECONDS = 60L
    }
}
