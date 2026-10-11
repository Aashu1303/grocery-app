package app.rasoi.core.domain.auth

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.testing.FakeAuthRepository
import app.rasoi.core.testing.FakePhoneAuthProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SignInUseCasesTest {
    private val provider = FakePhoneAuthProvider()
    private val auth = FakeAuthRepository()
    private val requestOtp = RequestOtpUseCase(provider)
    private val verifyOtp = VerifyOtpUseCase(provider, auth)
    private val completeProfile = CompleteProfileUseCase(auth)

    private fun invalid(field: String, code: String) =
        AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation(field, code))))

    @Test
    fun requestOtpNormalisesThePhone() = runTest {
        val request = (requestOtp("98123 45678") as AppResult.Success).value
        assertEquals("+919812345678", request.phone.e164)
        assertEquals("verification-+919812345678", request.verificationId)
    }

    @Test
    fun requestOtpRejectsInvalidPhonesWithoutCallingTheProvider() = runTest {
        assertEquals(invalid("phone", "invalid"), requestOtp("123"))
        assertTrue(provider.requested.isEmpty())
    }

    @Test
    fun requestOtpPassesProviderFailuresThrough() = runTest {
        provider.requestFailure = AppError.Offline
        assertEquals(AppResult.Failure(AppError.Offline), requestOtp("9812345678"))
    }

    @Test
    fun firstTimeUserNeedsAProfileThenSignsIn() = runTest {
        val outcome = (verifyOtp("verification-+919812345678", "123456") as AppResult.Success).value
        val needsProfile = assertIs<SignInOutcome.NeedsProfile>(outcome)
        assertEquals("token-+919812345678", needsProfile.idToken)

        val user = (completeProfile(needsProfile.idToken, " Meera ") as AppResult.Success).value
        assertEquals("Meera", user.displayName)
        assertIs<AuthState.SignedIn>(auth.state.value)
    }

    @Test
    fun returningUserSignsInDirectly() = runTest {
        completeProfile("token-+919812345678", "Meera")
        val outcome = (verifyOtp("verification-+919812345678", "12 34 56") as AppResult.Success).value
        assertEquals("Meera", assertIs<SignInOutcome.SignedIn>(outcome).account.displayName)
    }

    @Test
    fun codesMustBeSixDigitsAndCorrect() = runTest {
        assertEquals(invalid("code", "invalid"), verifyOtp("v", "12345"))
        assertEquals(invalid("code", "invalid"), verifyOtp("v", "12345a"))
        assertEquals(invalid("code", "incorrect"), verifyOtp("verification-+919812345678", "654321"))
    }

    @Test
    fun serverFailuresOtherThanMissingNameAreReturned() = runTest {
        auth.failure = AppError.Offline
        assertEquals(AppResult.Failure(AppError.Offline), verifyOtp("verification-+919812345678", "123456"))
    }

    @Test
    fun profileNameIsValidatedLocally() = runTest {
        assertEquals(invalid("displayName", "blank"), completeProfile("token", "  "))
        assertTrue(auth.signInCalls.isEmpty())
    }
}
