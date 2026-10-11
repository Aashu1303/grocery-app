package app.rasoi.core.model

import kotlinx.datetime.LocalDate

/** A calendar month such as `2026-10`, the label of a settlement period. */
data class PeriodMonth(val year: Int, val month: Int) : Comparable<PeriodMonth> {
    init {
        require(month in 1..MONTHS_IN_YEAR) { "month must be 1 to 12" }
    }

    fun next(): PeriodMonth = if (month == MONTHS_IN_YEAR) PeriodMonth(year + 1, 1) else PeriodMonth(year, month + 1)

    override fun compareTo(other: PeriodMonth): Int =
        compareValuesBy(this, other, PeriodMonth::year, PeriodMonth::month)

    override fun toString(): String =
        "${year.toString().padStart(YEAR_DIGITS, '0')}-${month.toString().padStart(2, '0')}"

    companion object {
        private const val MONTHS_IN_YEAR = 12
        private const val YEAR_DIGITS = 4
        private val FORMAT = Regex("""^(\d{4})-(\d{2})$""")

        fun of(date: LocalDate): PeriodMonth = PeriodMonth(date.year, date.month.ordinal + 1)

        /** Parses `YYYY-MM`; null for anything else. */
        fun parse(text: String): PeriodMonth? {
            val (year, month) = FORMAT.matchEntire(text)?.destructured ?: return null
            return month.toInt().takeIf { it in 1..MONTHS_IN_YEAR }?.let { PeriodMonth(year.toInt(), it) }
        }
    }
}
