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
 * Instamart (Swinsta franchise) tax invoices. Descriptions wrap above and below the numbered row; the
 * whole-rupee Total column is what was paid (it sums to the Invoice Value), and fees follow the table.
 */
class InstamartInvoiceParser : InvoiceParser {
    override val platform = Platform.INSTAMART

    override fun matches(document: ExtractedDocument): Boolean {
        val text = document.plainText
        return text.contains("Swinsta", ignoreCase = true) || text.contains("Instamart", ignoreCase = true)
    }

    override fun parse(document: ExtractedDocument): AppResult<ParsedInvoice> = parsing {
        val layout = Layout(document)
        val invoiceNumber = layout.valueAfter("Invoice No:").orFail("invoice_number")
        val date = layout.valueAfter("Date of Invoice:")?.let(::parseDate).orFail("date")
        val header = layout.lines.firstOrNull { it.contains("Description") && it.contains("Quantity") }.orFail("table")
        val serialRight = layout.lines.filter { it.page == header.page }.flatMap { it.words }
            .firstOrNull { it.text == "Sr" }.orFail("table").right + SLACK
        val quantityX = header.words.firstOrNull { it.text == "Quantity" }.orFail("table").x - SLACK
        val headerBottom = layout.find("(Rs.)").orFail("table").y
        val tableEnd = layout.lines.indexOf(layout.find("Invoice Value").orFail("total"))
        val body = layout.lines.subList(0, tableEnd).filter { it.page == header.page && it.y > headerBottom }
        val rows = DescriptionAssigner.assign(
            lines = body,
            isAnchor = { line ->
                line.words.firstOrNull()?.let { SERIAL.matches(it.text) && it.x < serialRight } == true
            },
            description = { line ->
                line.words.filter { it.x > serialRight && it.x < quantityX }.joinToString(" ") { it.text }
            },
            alignment = CellAlignment.CENTERED,
        )
        val lines = rows.map { parseRow(it, quantityX).orFail("lines") }
        val (charges, total) = summary(layout.lines.drop(tableEnd + 1))
        ParsedInvoice(
            platform = platform,
            invoiceNumber = invoiceNumber,
            orderId = layout.valueAfter("Order ID:"),
            invoiceDate = date,
            lines = lines,
            charges = charges,
            total = total.orFail("total"),
        )
    }

    /** Fee rows after the table carry a label; the grand total is the lone amount after them. */
    private fun summary(lines: List<Line>): Pair<List<ParsedCharge>, Money?> {
        val charges = mutableListOf<ParsedCharge>()
        var total: Money? = null
        lines.takeWhile { !it.contains("ANNEXURE") && !it.contains("Amount in words") }.forEach { line ->
            val amount = parseAmount(line.words.last().text) ?: return@forEach
            val label = line.words.dropLast(1).joinToString(" ") { it.text }
            if (label.isEmpty()) {
                total = amount
            } else {
                charges += ParsedCharge(ChargeClassifier.classify(label, amount), label, amount)
            }
        }
        return charges to total
    }

    private fun parseRow(row: TableRow, quantityX: Float): ParsedLine? {
        val fields = row.anchor.words.filter { it.x >= quantityX }.map { it.text }
        val units = fields.firstOrNull()?.toIntOrNull() ?: return null
        val total = fields.lastOrNull()?.let(::parseAmount) ?: return null
        return ParsedLine(row.description, units, total, hsn = fields.getOrNull(HSN_INDEX))
    }

    private companion object {
        val SERIAL = Regex("""^\d+\.$""")
        const val SLACK = 2f
        const val HSN_INDEX = 2
    }
}
