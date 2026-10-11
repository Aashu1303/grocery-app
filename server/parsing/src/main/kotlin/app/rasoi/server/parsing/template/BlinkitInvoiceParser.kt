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
import app.rasoi.server.parsing.model.ParsedInvoice
import app.rasoi.server.parsing.model.ParsedLine

/**
 * Blinkit tax invoices. Each item row starts with its serial number; the UPC and the `(HSN-…)` code wrap onto
 * the next line. A line is paid at its tax-inclusive Total.
 */
class BlinkitInvoiceParser : InvoiceParser {
    override val platform = Platform.BLINKIT

    override fun matches(document: ExtractedDocument): Boolean =
        document.plainText.contains("Blink Commerce", ignoreCase = true)

    override fun parse(document: ExtractedDocument): AppResult<ParsedInvoice> = parsing {
        val layout = Layout(document)
        val invoiceNumber = layout.valueAfter("Invoice Number:").orFail("invoice_number")
        val date = layout.valueAfter("Invoice Date")?.let(::parseDate).orFail("date")
        val header = layout.lines.firstOrNull { it.contains("UPC") && it.contains("Item") }.orFail("table")
        val columns = Columns(
            upc = header.words.firstOrNull { it.text == "UPC" }.orFail("table").x - SLACK,
            item = header.words.firstOrNull { it.text == "Item" }.orFail("table").x - SLACK,
            mrp = header.words.firstOrNull { it.text == "MRP" }.orFail("table").x - SLACK,
        )
        val body = layout.lines.dropWhile { it != header }.drop(1)
        val totalsRow = body
            .firstOrNull { line -> line.words.any { it.text == "Total" && it.x in columns.item..columns.mrp } }
            .orFail("total")
        val lines = groupRows(body.takeWhile { it != totalsRow }, columns).map { parseRow(it, columns).orFail("lines") }
        val total = parseAmount(totalsRow.words.last().text).orFail("total")
        ParsedInvoice(platform, invoiceNumber, layout.valueAfter("Order Id"), date, lines, emptyList(), total)
    }

    private data class Columns(val upc: Float, val item: Float, val mrp: Float)

    private fun groupRows(rows: List<Line>, columns: Columns): List<List<Line>> {
        val groups = mutableListOf<MutableList<Line>>()
        rows.forEach { line ->
            val first = line.words.firstOrNull()
            if (first != null && first.x < columns.upc && first.text.toIntOrNull() != null) {
                groups += mutableListOf(line)
            } else {
                groups.lastOrNull()?.add(line)
            }
        }
        return groups
    }

    private fun parseRow(group: List<Line>, columns: Columns): ParsedLine? {
        val upc = group.flatMap { line -> line.words.filter { it.x >= columns.upc && it.x < columns.item } }
            .joinToString("") { it.text }
        val descriptionWords = group.flatMap { line ->
            line.words.filter { it.x >= columns.item && it.x < columns.mrp }
        }
        val hsn = descriptionWords.firstNotNullOfOrNull { HSN.find(it.text)?.groupValues?.get(1) }
        val name = descriptionWords.filterNot { HSN.containsMatchIn(it.text) }.joinToString(" ") { it.text }
        val numbers = group.first().words.filter { it.x >= columns.mrp }.map { it.text }
        val mrp: Money? = numbers.firstOrNull()?.let(::parseAmount)
        val units = numbers.getOrNull(QTY_INDEX)?.toIntOrNull()
        val total = numbers.lastOrNull()?.let(::parseAmount)
        return if (mrp != null && units != null && total != null) {
            ParsedLine(name, units, total, mrp = mrp, hsn = hsn, upc = upc.ifEmpty { null })
        } else {
            null
        }
    }

    private companion object {
        val HSN = Regex("""\(HSN-(\d+)\)""")
        const val SLACK = 3f
        const val QTY_INDEX = 2
    }
}
