package app.rasoi.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PhoneNumberTest {
    @Test
    fun acceptsE164Numbers() {
        assertEquals("+919812345678", PhoneNumber.parse("+919812345678")?.e164)
        assertEquals("+14155550123", PhoneNumber.parse("+14155550123")?.e164)
    }

    @Test
    fun normalisesIndianInput() {
        assertEquals("+919812345678", PhoneNumber.parse("98123 45678")?.e164)
        assertEquals("+919812345678", PhoneNumber.parse("+91 98123-45678")?.e164)
        assertEquals("+919812345678", PhoneNumber.parse("09812345678")?.e164)
    }

    @Test
    fun rejectsInvalidNumbers() {
        listOf("", "12345", "+0123456789", "phone", "+91 98123 4567 89 123", "5812345678").forEach {
            assertNull(PhoneNumber.parse(it), "expected null for '$it'")
        }
    }

    @Test
    fun masksForDisplayWithoutLeakingTheNumber() {
        assertEquals("+91 •••••• 5678", PhoneNumber.parse("+919812345678")?.masked())
    }
}
