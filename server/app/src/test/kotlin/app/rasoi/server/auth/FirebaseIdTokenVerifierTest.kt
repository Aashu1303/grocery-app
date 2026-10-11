package app.rasoi.server.auth

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.PhoneNumber
import app.rasoi.server.support.MutableClock
import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.SigningKeyNotFoundException
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.coroutines.test.runTest
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaInstant

class FirebaseIdTokenVerifierTest {
    private val projectId = "rasoi-test"
    private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val otherKey = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val clock = MutableClock()

    private val keys = JwkProvider { keyId ->
        if (keyId != KEY_ID) throw SigningKeyNotFoundException("unknown $keyId", null)
        val public = keyPair.public as RSAPublicKey
        val encoder = Base64.getUrlEncoder().withoutPadding()
        Jwk.fromValues(
            mapOf(
                "kid" to KEY_ID,
                "kty" to "RSA",
                "alg" to "RS256",
                "use" to "sig",
                "n" to encoder.encodeToString(
                    public.modulus.toByteArray().dropWhile {
                        it == 0.toByte()
                    }.toByteArray(),
                ),
                "e" to encoder.encodeToString(public.publicExponent.toByteArray()),
            ),
        )
    }

    private val verifier = FirebaseIdTokenVerifier(projectId, keys, clock)

    private fun token(
        issuer: String = "https://securetoken.google.com/$projectId",
        audience: String = projectId,
        phone: String? = "+919812345678",
        expiresIn: kotlin.time.Duration = 1.hours,
        signingKey: RSAPrivateKey = keyPair.private as RSAPrivateKey,
        keyId: String = KEY_ID,
    ): String {
        val now = clock.now()
        return JWT.create()
            .withKeyId(keyId)
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject("firebase-uid-1")
            .withIssuedAt(now.toJavaInstant())
            .withExpiresAt((now + expiresIn).toJavaInstant())
            .apply { if (phone != null) withClaim("phone_number", phone) }
            .sign(Algorithm.RSA256(null, signingKey))
    }

    @Test
    fun validTokenYieldsTheIdentity() = runTest {
        val identity = VerifiedIdentity("firebase-uid-1", requireNotNull(PhoneNumber.parse("+919812345678")))
        assertEquals(AppResult.Success(identity), verifier.verify(token()))
    }

    @Test
    fun wrongAudienceIssuerOrSignatureIsRejected() = runTest {
        val rejected = AppResult.Failure(AppError.Unauthenticated)
        assertEquals(rejected, verifier.verify(token(audience = "other-project")))
        assertEquals(rejected, verifier.verify(token(issuer = "https://evil.example.com/$projectId")))
        assertEquals(rejected, verifier.verify(token(signingKey = otherKey.private as RSAPrivateKey)))
        assertEquals(rejected, verifier.verify(token(keyId = "unknown-key")))
        assertEquals(rejected, verifier.verify("not.a.jwt"))
    }

    @Test
    fun expiredTokenIsRejectedAfterLeeway() = runTest {
        val shortLived = token(expiresIn = 5.minutes)
        clock.advance(10.minutes)
        assertEquals(AppResult.Failure(AppError.Unauthenticated), verifier.verify(shortLived))
    }

    @Test
    fun tokenWithoutAPhoneNumberIsRejected() = runTest {
        assertEquals(AppResult.Failure(AppError.Unauthenticated), verifier.verify(token(phone = null)))
    }

    private companion object {
        const val KEY_ID = "test-key"
    }
}
