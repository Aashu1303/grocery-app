package app.rasoi.server.parsing.layout

import app.rasoi.core.model.Money
import kotlinx.datetime.LocalDate

private val DMY = Regex("""^(\d{2})-(\d{2})-(\d{4})$""")
private val YMD = Regex("""^(\d{4})-(\d{2})-(\d{2})$""")
private val CURRENCY = Regex("""^-?(Rs\.?|₹)$""", RegexOption.IGNORE_CASE)

/** Parses an amount written as `26.00`, `Rs 20.0`, `-Rs 1.0` or `₹1,234.50` (the words of one amount). */
fun parseAmount(words: List<String>): Money? {
    val negative = words.firstOrNull()?.startsWith("-") == true
    val digits = words.filterNot { CURRENCY.matches(it) }.joinToString("").removePrefix("-").removePrefix("₹")
    val value = Money.parse(digits) ?: return null
    return if (negative) -value else value
}

fun parseAmount(text: String): Money? = parseAmount(text.trim().split(Regex("\\s+")))

/** Dates printed as `04-10-2026` (day first) or `2026-10-02`. */
fun parseDate(text: String): LocalDate? {
    DMY.matchEntire(text)?.destructured?.let { (d, m, y) ->
        return runCatching { LocalDate(y.toInt(), m.toInt(), d.toInt()) }.getOrNull()
    }
    YMD.matchEntire(text)?.destructured?.let { (y, m, d) ->
        return runCatching { LocalDate(y.toInt(), m.toInt(), d.toInt()) }.getOrNull()
    }
    return null
}
