package app.rasoi.server.parsing.fallback

import app.rasoi.server.parsing.layout.Line

/**
 * The only invoice text that may go to the LLM (design-plan section 2): the item table, from the first line with
 * two amounts to the last line with an amount. Lines that carry personal labels are dropped, and long digit runs
 * and email addresses are masked.
 */
internal object LineItemExcerpt {
    const val MAX_LENGTH = 12_000
    private const val MASK = "[redacted]"
    private const val MIN_PHONE_DIGITS = 10
    private val DECIMAL = Regex("""^-?(?:Rs\.?|₹)?-?\d{1,3}(?:,?\d{2,3})*\.\d{1,2}$""", RegexOption.IGNORE_CASE)
    private val CURRENCY = Regex("""^-?(?:Rs\.?|₹)$""", RegexOption.IGNORE_CASE)
    private val EMAIL = Regex("""\S+@\S+""")
    private val PERSONAL = Regex(
        """\b(name|address|customer|phone|mobile|e-?mail|ship to|bill to|deliver to|gstin|contact|pin ?code)\b""",
        RegexOption.IGNORE_CASE,
    )

    fun of(lines: List<Line>): String? {
        val start = lines.indexOfFirst { amounts(it) >= 2 }
        val end = lines.indexOfLast { amounts(it) >= 1 }
        if (start < 0 || end < start) return null
        return lines.subList(start, end + 1)
            .filterNot { PERSONAL.containsMatchIn(it.text) }
            .joinToString("\n") { line -> line.words.joinToString(" ") { mask(it.text) } }
            .takeIf { it.length <= MAX_LENGTH }
    }

    private fun amounts(line: Line): Int = line.words.withIndex().count { (index, word) ->
        DECIMAL.matches(word.text) ||
            (index > 0 && CURRENCY.matches(line.words[index - 1].text) && word.text.any(Char::isDigit))
    }

    private fun mask(word: String): String = when {
        EMAIL.matches(word) -> MASK
        word.count(Char::isDigit) >= MIN_PHONE_DIGITS -> MASK
        else -> word
    }
}
