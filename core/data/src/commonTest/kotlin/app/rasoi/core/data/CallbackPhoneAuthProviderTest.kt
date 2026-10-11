package app.rasoi.core.data

import app.rasoi.core.data.auth.CallbackPhoneAuthProvider
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.PhoneNumber
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CallbackPhoneAuthProviderTest {
    private val phone = requireNotNull(PhoneNumber.parse("+919812345678"))

    private fun provider(requestResult: Pair<String?, String?>, confirmResult: Pair<String?, String?>) =
        CallbackPhoneAuthProvider(
            requestCode = { _, done -> done(requestResult.first, requestResult.second) },
            confirmCode = { _, _, done -> done(confirmResult.first, confirmResult.second) },
        )

    @Test
    fun returnsTheVerificationIdAndTheIdToken() = runTest {
        val provider = provider("verification" to null, "id-token" to null)
        assertEquals(AppResult.Success("verification"), provider.requestCode(phone))
        assertEquals(AppResult.Success("id-token"), provider.confirm("verification", "123456"))
    }

    @Test
    fun mapsARejectedNumberAndARejectedCodeToFieldErrors() = runTest {
        val provider = provider(null to "invalid", null to "invalid")
        val number = AppError.Validation(listOf(AppError.FieldViolation("phone", "invalid")))
        val code = AppError.Validation(listOf(AppError.FieldViolation("code", "incorrect")))
        assertEquals(AppResult.Failure(number), provider.requestCode(phone))
        assertEquals(AppResult.Failure(code), provider.confirm("verification", "000000"))
    }

    @Test
    fun mapsNetworkAndUnknownFailures() = runTest {
        assertEquals(AppResult.Failure(AppError.Offline), provider(null to "offline", null to null).requestCode(phone))
        assertEquals(
            AppResult.Failure(AppError.Unexpected("boom")),
            provider(null to null, null to "boom").confirm("verification", "123456"),
        )
    }

    @Test
    fun anAnswerWithNeitherValueNorErrorIsAFailure() = runTest {
        val result = provider(null to null, null to null).requestCode(phone)
        assertEquals(AppResult.Failure(AppError.Unexpected("empty_result")), result)
    }

    @Test
    fun aSecondAnswerFromTheSameCallIsIgnored() = runTest {
        val twice = CallbackPhoneAuthProvider(
            requestCode = { _, done ->
                done("first", null)
                done("second", null)
            },
            confirmCode = { _, _, _ -> },
        )
        assertEquals(AppResult.Success("first"), twice.requestCode(phone))
    }
}
