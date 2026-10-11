package app.rasoi.core.domain.ledger

import app.rasoi.core.model.PeriodMonth
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class PeriodRulesTest {
    private val kolkata = TimeZone.of("Asia/Kolkata")

    @Test
    fun theMonthIsReadInTheHouseholdsTimeZone() {
        // 19:00 UTC on 31 October is 00:30 on 1 November in Kolkata.
        val instant = Instant.parse("2026-10-31T19:00:00Z")
        assertEquals(LocalDate(2026, 11, 1), PeriodRules.today(instant, kolkata))
        assertEquals(PeriodMonth(2026, 11), PeriodRules.monthOf(instant, kolkata))
        assertEquals(PeriodMonth(2026, 10), PeriodRules.monthOf(instant, TimeZone.UTC))
    }

    @Test
    fun aPeriodIsDueOnceItsMonthHasEnded() {
        val october = PeriodMonth(2026, 10)
        assertEquals(false, PeriodRules.isDue(october, LocalDate(2026, 10, 31)))
        assertEquals(true, PeriodRules.isDue(october, LocalDate(2026, 11, 1)))
        assertEquals(true, PeriodRules.isDue(october, LocalDate(2027, 3, 9)))
    }

    @Test
    fun theNextPeriodFollowsTheClosedOneButNeverLagsToday() {
        val october = PeriodMonth(2026, 10)
        // Closed on time or early: the next month. Closed months late: today's month.
        assertEquals(PeriodMonth(2026, 11), PeriodRules.successor(october, LocalDate(2026, 11, 1)))
        assertEquals(PeriodMonth(2026, 11), PeriodRules.successor(october, LocalDate(2026, 10, 12)))
        assertEquals(PeriodMonth(2027, 2), PeriodRules.successor(october, LocalDate(2027, 2, 3)))
    }
}
