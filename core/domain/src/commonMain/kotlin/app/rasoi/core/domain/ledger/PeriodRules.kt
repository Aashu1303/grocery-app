package app.rasoi.core.domain.ledger

import app.rasoi.core.model.PeriodMonth
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Design-plan 6.3: periods are months in the household's time zone and close when the month ends, or early. */
object PeriodRules {
    fun today(now: Instant, zone: TimeZone): LocalDate = now.toLocalDateTime(zone).date

    fun monthOf(now: Instant, zone: TimeZone): PeriodMonth = PeriodMonth.of(today(now, zone))

    /** The month of [period] has ended, so the close job should lock it. */
    fun isDue(period: PeriodMonth, today: LocalDate): Boolean = PeriodMonth.of(today) > period

    /** The month the next period carries: the one after [closing], or today's month if that is later. */
    fun successor(closing: PeriodMonth, today: LocalDate): PeriodMonth = maxOf(closing.next(), PeriodMonth.of(today))
}
