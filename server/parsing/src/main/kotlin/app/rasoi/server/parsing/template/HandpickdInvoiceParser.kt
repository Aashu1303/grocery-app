package app.rasoi.server.parsing.template

import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Money
import app.rasoi.core.model.Platform
import app.rasoi.server.parsing.InvoiceParser
import app.rasoi.server.parsing.extract.ExtractedDocument
import app.rasoi.server.parsing.layout.Layout
import app.rasoi.server.parsing.layout.Line
import app.rasoi.server.parsing.layout.parseAmount
import app.rasoi.server.parsing.layout.parseDate
import app.rasoi.server.parsing.model.ParsedCharge
import app.rasoi.server.parsing.model.ParsedInvoice
import app.rasoi.server.parsing.model.ParsedLine

/**
 * Handpickd delivery bills (not GST invoices). The brand name is only in the logo image, so detection uses the
 * bill's own structure (ADR 0017). A line is paid at its amount minus its promo discount; summary rows after the
 * items become charges, except the items and promo subtotals already reflected in the lines.
 */
class HandpickdInvoiceParser : InvoiceParser {
    override val platform = Platform.HANDPICKD

    override fun matches(document: ExtractedDocument): Boolean {
        val text = document.plainText
        return "Bill Total" in text && ORDER.containsMatchIn(text) && "/Kilogram" in text
    }

    override fun parse(document: ExtractedDocument): AppResult<ParsedInvoice> = parsing {
        val layout = Layout(document)
        val order = layout.valueAfter("Order:").orFail("order")
        val date = layout.valueAfter("Delivery Date:")?.let(::parseDate).orFail("date")
        val header = layout.lines.firstOrNull { it.contains("ITEM") && it.contains("AMOUNT") }.orFail("table")
        val amountX = header.words.firstOrNull { it.text == "AMOUNT" }.orFail("table").x - COLUMN_SLACK
        val body = layout.lines.dropWhile { it != header }.drop(1)
        val itemLines = body.takeWhile { !it.contains("Items Total") }
        val summary = body.drop(itemLines.size)

        val lines = items(itemLines, amountX).orFail("lines")
        val charges = mutableListOf<ParsedCharge>()
        var total: Money? = null
        summary.forEach { line ->
            val label = line.words.filter { it.x < amountX }.joinToString(" ") { it.text }
            val amount = parseAmount(line.words.filter { it.x >= amountX }.map { it.text }) ?: return@forEach
            when {
                label.equals("Bill Total", ignoreCase = true) -> total = amount
                label.endsWith("Items Total", ignoreCase = true) || label.contains("Promo", ignoreCase = true) -> Unit
                else -> charges += ParsedCharge(ChargeClassifier.classify(label, amount), label, amount)
            }
        }
        ParsedInvoice(
            platform = platform,
            invoiceNumber = order,
            orderId = order,
            invoiceDate = date,
            lines = lines,
            charges = charges,
            total = total.orFail("total"),
        )
    }

    private fun items(lines: List<Line>, amountX: Float): List<ParsedLine>? {
        val result = mutableListOf<ParsedLine>()
        lines.forEach { line ->
            val first = line.words.firstOrNull() ?: return@forEach
            val amountWords = line.words.filter { it.x >= amountX }.map { it.text }
            val labelWords = line.words.filter { it.x < amountX && it.x > first.right }.map { it.text }
            when {
                first.text.toIntOrNull() != null && first.x < INDEX_MAX_X -> {
                    val amount = parseAmount(amountWords) ?: return null
                    result += ParsedLine(rawName = labelWords.joinToString(" "), units = 1, amount = amount)
                }
                result.isNotEmpty() && line.contains("Promo Discount") -> {
                    val discount = parseAmount(amountWords) ?: return null
                    result[result.lastIndex] = result.last().let { it.copy(amount = it.amount + discount) }
                }
                result.isNotEmpty() && amountWords.size == 2 -> {
                    parseMeasure(amountWords[0], amountWords[1])?.let { measure ->
                        result[result.lastIndex] = result.last().copy(measure = measure)
                    }
                }
            }
        }
        return result
    }

    private companion object {
        val ORDER = Regex("""Order:\s+OD-""")
        const val COLUMN_SLACK = 5f
        const val INDEX_MAX_X = 60f
    }
}
