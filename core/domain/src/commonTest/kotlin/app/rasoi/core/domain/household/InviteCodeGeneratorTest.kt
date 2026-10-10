package app.rasoi.core.domain.household

import app.rasoi.core.model.InviteCode
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InviteCodeGeneratorTest {
    private val generator = InviteCodeGenerator(Random(seed = 42))

    @Test
    fun codesHaveFixedLengthAndUnambiguousAlphabet() {
        repeat(1_000) {
            val code = generator.next().value
            assertEquals(InviteCodeGenerator.LENGTH, code.length)
            assertTrue(code.all { it in InviteCodeGenerator.ALPHABET }, code)
            assertFalse(code.any { it in "01IO" }, code)
        }
    }

    @Test
    fun codesAreUniqueInPractice() {
        val codes = List(10_000) { generator.next() }.toSet()
        assertEquals(10_000, codes.size)
    }

    @Test
    fun normalisesUserInput() {
        assertEquals(InviteCode("ABCD2345"), InviteCodeGenerator.normalise(" abcd-2345 "))
        assertNull(InviteCodeGenerator.normalise("ABC"))
        assertNull(InviteCodeGenerator.normalise("ABCD234O"))
    }
}
