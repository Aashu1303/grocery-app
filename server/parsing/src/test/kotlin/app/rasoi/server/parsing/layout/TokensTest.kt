package app.rasoi.server.parsing.layout

import app.rasoi.core.model.Money
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TokensTest {
    @Test
    fun parsesPlainAndCurrencyPrefixedAmounts() {
        assertEquals(Money(2600), parseAmount("26.00"))
        assertEquals(Money(2000), parseAmount("Rs 20.0"))
        assertEquals(Money(2000), parseAmount("Rs. 20"))
        assertEquals(Money(123450), parseAmount("₹1,234.50"))
        assertEquals(Money(123450), parseAmount("₹ 1,234.50"))
    }

    @Test
    fun keepsTheSignOfNegativeAmounts() {
        assertEquals(Money(-100), parseAmount("-Rs 1.0"))
        assertEquals(Money(-4250), parseAmount("-42.50"))
        assertEquals(Money(-4250), parseAmount(listOf("-₹", "42.50")))
    }

    @Test
    fun rejectsTextThatIsNotAnAmount() {
        assertNull(parseAmount("Total"))
        assertNull(parseAmount("12.345"))
        assertNull(parseAmount(""))
    }

    @Test
    fun parsesDayFirstAndIsoDates() {
        assertEquals(LocalDate(2026, 10, 4), parseDate("04-10-2026"))
        assertEquals(LocalDate(2026, 10, 2), parseDate("2026-10-02"))
    }

    @Test
    fun rejectsImpossibleOrUnknownDates() {
        assertNull(parseDate("31-02-2026"))
        assertNull(parseDate("2026-13-01"))
        assertNull(parseDate("4/10/2026"))
        assertNull(parseDate("Oct 4, 2026"))
    }
}
