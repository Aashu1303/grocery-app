package app.rasoi.server.parsing.fallback

import app.rasoi.server.parsing.layout.Line
import app.rasoi.server.parsing.layout.parseDate
import kotlinx.datetime.LocalDate

/** Header fields read locally, so the customer block never has to leave the server. */
internal data class InvoiceHeader(val invoiceNumber: String?, val orderId: String?, val date: LocalDate?) {
    companion object {
        private val INVOICE_LABELS = listOf("invoice no", "invoice number", "invoice #", "bill no", "bill number")
        private val ORDER_LABELS = listOf("order id", "order no", "order number", "order")
        private val DATE_LABELS = listOf("invoice date", "date of invoice", "delivery date", "date")
        private const val MAX_REFERENCE_LENGTH = 40

        fun read(lines: List<Line>): InvoiceHeader = InvoiceHeader(
            invoiceNumber = reference(lines, INVOICE_LABELS),
            orderId = reference(lines, ORDER_LABELS),
            date = valueAfter(lines, DATE_LABELS) { parseDate(it) }
                ?: lines.firstNotNullOfOrNull { line -> line.words.firstNotNullOfOrNull { parseDate(it.text) } },
        )

        private fun reference(lines: List<Line>, labels: List<String>): String? = valueAfter(lines, labels) { value ->
            value.takeIf { it.length <= MAX_REFERENCE_LENGTH && it.any(Char::isDigit) }
        }

        /** The first word after any of [labels] (ignoring case and trailing `:`, `.`) that [accept] maps to a value. */
        private fun <T : Any> valueAfter(lines: List<Line>, labels: List<String>, accept: (String) -> T?): T? {
            labels.forEach { label ->
                val labelWords = label.split(' ')
                lines.forEach { line ->
                    val texts = line.words.map { it.text }
                    val keys = texts.map { it.lowercase().trimEnd(':', '.') }
                    keys.windowed(labelWords.size).forEachIndexed { index, window ->
                        if (window == labelWords) {
                            val value = texts.drop(index + labelWords.size).firstOrNull { it != ":" }
                            value?.let(accept)?.let { return it }
                        }
                    }
                }
            }
            return null
        }
    }
}
