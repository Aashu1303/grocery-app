package app.rasoi.core.data

import app.rasoi.core.data.auth.DevPhoneAuthProvider
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.PhoneNumber
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DevPhoneAuthProviderTest {
    private val provider = DevPhoneAuthProvider()

    @Test
    fun producesServerDevTokensForTheFixedCode() = runTest {
        val phone = requireNotNull(PhoneNumber.parse("+919812345678"))
        val verification = (provider.requestCode(phone) as AppResult.Success).value
        val confirmed = provider.confirm(verification, DevPhoneAuthProvider.DEV_CODE)
        assertEquals(AppResult.Success("dev:+919812345678"), confirmed)
        assertIs<AppResult.Failure>(provider.confirm(verification, "000000"))
    }
}
