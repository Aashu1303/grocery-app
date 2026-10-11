package app.rasoi.server.auth

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DevIdTokenVerifierTest {
    private val verifier = DevIdTokenVerifier()

    @Test
    fun acceptsOnlyDevPrefixedValidPhones() = runTest {
        val identity = (verifier.verify("dev:+919812345678") as AppResult.Success).value
        assertEquals("dev-+919812345678", identity.firebaseUid)
        assertEquals(AppResult.Failure(AppError.Unauthenticated), verifier.verify("+919812345678"))
        assertEquals(AppResult.Failure(AppError.Unauthenticated), verifier.verify("dev:12"))
    }
}
