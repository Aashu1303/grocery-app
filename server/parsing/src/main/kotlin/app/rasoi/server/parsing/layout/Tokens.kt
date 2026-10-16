package app.rasoi.server.parsing.layout

import app.rasoi.core.model.Money
import kotlinx.datetime.LocalDate

private val DMY = Regex("""^(\d{1,2})[-/.](\d{1,2})[-/.](\d{4})$""")
private val YMD = Regex("""^(\d{4})-(\d{2})-(\d{2})$""")
private val DAY_MONTH_NAME = Regex("""^(\d{1,2})-([A-Za-z]{3})-(\d{4})$""")
private val MONTHS = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
private val CURRENCY = Regex("""^-?(Rs\.?|₹)$""", RegexOption.IGNORE_CASE)

/** Parses an amount written as `26.00`, `Rs 20.0`, `-Rs 1.0` or `₹1,234.50` (the words of one amount). */
fun parseAmount(words: List<String>): Money? {
    val negative = words.firstOrNull()?.startsWith("-") == true
    val digits = words.filterNot { CURRENCY.matches(it) }.joinToString("").removePrefix("-").removePrefix("₹")
    val value = Money.parse(digits) ?: return null
    return if (negative) -value else value
}

fun parseAmount(text: String): Money? = parseAmount(text.trim().split(Regex("\\s+")))

/** Dates printed day first (`04-10-2026`, `4/10/2026`, `04.10.2026`, `14-Oct-2026`) or as ISO `2026-10-02`. */
fun parseDate(text: String): LocalDate? =
    DMY.matchEntire(text)?.destructured?.let { (d, m, y) -> date(y, m.toIntOrNull(), d) }
        ?: DAY_MONTH_NAME.matchEntire(text)?.destructured?.let { (d, name, y) ->
            date(y, MONTHS.indexOf(name.lowercase()).takeIf { it >= 0 }?.plus(1), d)
        }
        ?: YMD.matchEntire(text)?.destructured?.let { (y, m, d) -> date(y, m.toIntOrNull(), d) }

private fun date(year: String, month: Int?, day: String): LocalDate? =
    month?.let { runCatching { LocalDate(year.toInt(), it, day.toInt()) }.getOrNull() }
