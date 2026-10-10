package app.rasoi.core.model

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoneyTest {
    @Test
    fun arithmeticIsExactInPaise() {
        val a = Money(45_700)
        val b = Money(1_201)
        assertEquals(Money(46_901), a + b)
        assertEquals(Money(44_499), a - b)
        assertEquals(Money(-45_700), -a)
        assertEquals(Money(137_100), a * 3)
    }

    @Test
    fun rupeesFactoryConvertsToPaise() {
        assertEquals(Money(12_300), Money.ofRupees(123))
    }

    @Test
    fun overflowFailsInsteadOfWrapping() {
        assertFailsWith<ArithmeticException> { Money(Long.MAX_VALUE) + Money(1) }
        assertFailsWith<ArithmeticException> { Money(Long.MIN_VALUE) - Money(1) }
        assertFailsWith<ArithmeticException> { Money(Long.MAX_VALUE / 2 + 1) * 2 }
        assertFailsWith<ArithmeticException> { -Money(Long.MIN_VALUE) }
    }

    @Test
    fun comparisonFollowsPaise() {
        assertTrue(Money(1) > Money.ZERO)
        assertTrue(Money(-1).isNegative)
        assertTrue(Money.ZERO.isZero)
    }

    @Test
    fun sumOfEmptyListIsZero() {
        assertEquals(Money.ZERO, emptyList<Money>().sum())
        assertEquals(Money(30), listOf(Money(10), Money(20)).sum())
    }

    @Test
    fun parsesDecimalStringsExactly() {
        assertEquals(Money(46_901), Money.parse("469.01"))
        assertEquals(Money(1_201), Money.parse("12.01"))
        assertEquals(Money(1_250), Money.parse("12.5"))
        assertEquals(Money(45_700), Money.parse("457"))
        assertEquals(Money(-100), Money.parse("-1.0"))
        assertEquals(Money(12_345_678), Money.parse("1,23,456.78"))
        assertEquals(Money(1), Money.parse("0.01"))
        assertEquals(Money(5), Money.parse(".05"))
    }

    @Test
    fun rejectsMalformedStrings() {
        listOf("", " ", "abc", "1.234", "1..2", "--1", "1-", "₹10", "1e3", ".", "-", ",", "1,").forEach {
            assertNull(Money.parse(it), "expected null for '$it'")
        }
    }

    @Test
    fun formatsWithIndianGrouping() {
        assertEquals("₹0.00", Money.ZERO.format())
        assertEquals("₹0.05", Money(5).format())
        assertEquals("₹129.00", Money(12_900).format())
        assertEquals("₹1,234.50", Money(123_450).format())
        assertEquals("₹12,345.00", Money(1_234_500).format())
        assertEquals("₹1,23,456.00", Money(12_345_600).format())
        assertEquals("₹12,34,56,789.10", Money(123_456_789_10).format())
        assertEquals("-₹1,234.50", Money(-123_450).format())
    }

    @Test
    fun formatsExtremes() {
        assertEquals("-₹92,23,37,20,36,85,47,758.08", Money(Long.MIN_VALUE).format())
        assertEquals("₹92,23,37,20,36,85,47,758.07", Money(Long.MAX_VALUE).format())
    }

    @Test
    fun parseIsTheInverseOfPlainFormatting() {
        val random = Random(seed = 7)
        repeat(1_000) {
            val money = Money(random.nextLong(-10_000_000_000, 10_000_000_000))
            val plain = money.format().replace("₹", "")
            assertEquals(money, Money.parse(plain))
        }
    }
}
