package app.rasoi.core.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PeriodMonthTest {
    @Test
    fun formatsAndParsesYearAndMonth() {
        assertEquals("2026-10", PeriodMonth(2026, 10).toString())
        assertEquals("2026-01", PeriodMonth(2026, 1).toString())
        assertEquals(PeriodMonth(2026, 3), PeriodMonth.parse("2026-03"))
    }

    @Test
    fun rejectsAnythingThatIsNotAMonth() {
        listOf("2026-13", "2026-00", "26-10", "2026/10", "2026-1", "2026-10-01", "", "abcd-ef").forEach {
            assertNull(PeriodMonth.parse(it), it)
        }
        assertFailsWith<IllegalArgumentException> { PeriodMonth(2026, 13) }
    }

    @Test
    fun theNextMonthRollsOverTheYear() {
        assertEquals(PeriodMonth(2026, 11), PeriodMonth(2026, 10).next())
        assertEquals(PeriodMonth(2027, 1), PeriodMonth(2026, 12).next())
    }

    @Test
    fun aDateBelongsToItsMonth() {
        assertEquals(PeriodMonth(2026, 10), PeriodMonth.of(LocalDate(2026, 10, 31)))
        assertEquals(PeriodMonth(2027, 1), PeriodMonth.of(LocalDate(2027, 1, 1)))
    }

    @Test
    fun monthsOrderByYearThenMonth() {
        assertTrue(PeriodMonth(2026, 12) < PeriodMonth(2027, 1))
        assertTrue(PeriodMonth(2026, 9) < PeriodMonth(2026, 10))
        assertEquals(
            listOf(PeriodMonth(2026, 2), PeriodMonth(2026, 11)),
            listOf(PeriodMonth(2026, 11), PeriodMonth(2026, 2)).sorted(),
        )
    }
}
