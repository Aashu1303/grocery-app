package app.rasoi.server.parsing.template

import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Money
import app.rasoi.core.model.Platform
import app.rasoi.server.parsing.InvoiceParser
import app.rasoi.server.parsing.extract.ExtractedDocument
import app.rasoi.server.parsing.layout.CellAlignment
import app.rasoi.server.parsing.layout.DescriptionAssigner
import app.rasoi.server.parsing.layout.Layout
import app.rasoi.server.parsing.layout.Line
import app.rasoi.server.parsing.layout.TableRow
import app.rasoi.server.parsing.layout.parseAmount
import app.rasoi.server.parsing.layout.parseDate
import app.rasoi.server.parsing.model.ParsedCharge
import app.rasoi.server.parsing.model.ParsedInvoice
import app.rasoi.server.parsing.model.ParsedLine

/**
 * Firstclub tax invoices, issued by its seller Trolleypop Retail. A row starts on the line with its serial number and
 * its description continues below; the quantity is printed at the end of the description ("… x 2"). The last column,
 * Total, is what was paid for the line (tax included) and the lines sum to Item Total. Fees and coupons follow the
 * table as labelled rows, coupons printed without a sign, and Invoice value is the grand total.
 */
class FirstclubInvoiceParser : InvoiceParser {
    override val platform = Platform.FIRSTCLUB

    override fun matches(document: ExtractedDocument): Boolean =
        document.plainText.contains("TROLLEYPOP RETAIL", ignoreCase = true)

    override fun parse(document: ExtractedDocument): AppResult<ParsedInvoice> = parsing {
        val layout = Layout(document)
        val invoiceNumber = layout.valueAfter("Invoice No:").orFail("invoice_number")
        val date = layout.valueAfter("Date :")?.let(::parseDate).orFail("date")
        val header = layout.lines.indexOfFirst { it.contains("Particulars") }.takeIf { it >= 0 }.orFail("table")
        val tableEnd = layout.lines.indexOfFirst { it.words.firstOrNull()?.text == "Total" }
            .takeIf { it > header }.orFail("total")
        val body = layout.lines.subList(header + 1, tableEnd).filterNot(::isTableHeading)
        val rows = DescriptionAssigner.assign(
            lines = body,
            isAnchor = { line -> SERIAL.matches(line.words.first().text) && line.words.any { HSN.matches(it.text) } },
            description = { line -> descriptionWords(line).joinToString(" ") { it.text } },
            alignment = CellAlignment.TOP,
        )
        val lines = rows.map { parseRow(it).orFail("lines") }
        val (charges, total) = summary(layout.lines.drop(tableEnd + 1))
        ParsedInvoice(
            platform = platform,
            invoiceNumber = invoiceNumber,
            orderId = layout.valueAfter("Order No:"),
            invoiceDate = date,
            lines = lines,
            charges = charges,
            total = total.orFail("total"),
        )
    }

    /** A row's description: on its first line the words between the serial number and the HSN code. */
    private fun descriptionWords(line: Line) = if (SERIAL.matches(line.words.first().text)) {
        line.words.drop(1).takeWhile { !HSN.matches(it.text) }
    } else {
        line.words
    }

    /** Column headings, repeated when the table continues on another page, are not part of any description. */
    private fun isTableHeading(line: Line) = line.words.any { it.text in HEADINGS }

    private fun parseRow(row: TableRow): ParsedLine? {
        val quantity = QUANTITY.find(row.description)
        val units = quantity?.groupValues?.get(1)?.toIntOrNull()
        val numbers = row.anchor.words.dropWhile { !HSN.matches(it.text) }
        val total = numbers.lastOrNull()?.let { parseAmount(it.text) }
        return if (quantity == null || units == null || total == null) {
            null
        } else {
            ParsedLine(row.description.removeRange(quantity.range).trim(), units, total, hsn = numbers.first().text)
        }
    }

    /** Labelled rows until Invoice value; zero rows are left out and coupons reduce the bill. */
    private fun summary(lines: List<Line>): Pair<List<ParsedCharge>, Money?> {
        val charges = mutableListOf<ParsedCharge>()
        for (line in lines) {
            val amount = line.words.lastOrNull()?.let { parseAmount(it.text) } ?: continue
            val label = line.words.dropLast(1).joinToString(" ") { it.text }
            when {
                label.equals("Invoice value", ignoreCase = true) -> return charges to amount
                label.equals("Item Total", ignoreCase = true) || amount.isZero -> Unit
                else -> {
                    val reduces = REDUCTIONS.any { it in label.lowercase() }
                    val signed = if (reduces && !amount.isNegative) -amount else amount
                    charges += ParsedCharge(ChargeClassifier.classify(label, signed), label, signed)
                }
            }
        }
        return charges to null
    }

    private companion object {
        val SERIAL = Regex("""^\d{1,3}$""")
        val HSN = Regex("""^\d{6,8}$""")
        val QUANTITY = Regex("""\s+x\s+(\d+)$""")
        val HEADINGS = setOf("Particulars", "Taxable", "Rate", "Sr.", "HSN", "Code", "Value")
        val REDUCTIONS = listOf("coupon", "discount")
    }
}
