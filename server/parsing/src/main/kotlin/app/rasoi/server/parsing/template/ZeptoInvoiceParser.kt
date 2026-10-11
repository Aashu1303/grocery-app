package app.rasoi.server.parsing.template

import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Money
import app.rasoi.core.model.Platform
import app.rasoi.core.model.sum
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
 * Zepto tax invoices: a multi-page table with the header repeated on each page and descriptions centred on
 * the numbered row. The printed line totals are rounded, so a line is paid at taxable value plus taxes, which
 * reconciles exactly with Item Total and the round-off (ADR 0017).
 */
class ZeptoInvoiceParser : InvoiceParser {
    override val platform = Platform.ZEPTO

    override fun matches(document: ExtractedDocument): Boolean {
        val text = document.plainText
        return text.contains("ZEPTO MARKETPLACE", ignoreCase = true) ||
            text.contains("Geddit Convenience", ignoreCase = true)
    }

    override fun parse(document: ExtractedDocument): AppResult<ParsedInvoice> = parsing {
        val layout = Layout(document)
        val invoiceNumber = layout.valueAfter("Invoice No.:").orFail("invoice_number")
        val date = layout.valueAfter("Date")?.let(::parseDate).orFail("date")
        val endIndex = layout.lines.indexOf(layout.find("Item Total").orFail("total"))
        val pages = layout.lines.filter { it.contains("Description") }.associateBy { it.page }
            .mapValues { (_, header) -> pageColumns(layout, header).orFail("table") }
        val body = layout.lines.subList(0, endIndex).filter { line ->
            pages[line.page]?.let { line.y > it.headerBottom } == true
        }
        val rows = DescriptionAssigner.assign(
            lines = body,
            isAnchor = { line -> isAnchor(line, pages.getValue(line.page)) },
            description = { line -> description(line, pages.getValue(line.page)) },
            alignment = CellAlignment.CENTERED,
        )
        val lines = rows.map { parseRow(it).orFail("lines") }
        val (charges, total) = footer(layout.lines.drop(endIndex + 1))
        ParsedInvoice(
            platform = platform,
            invoiceNumber = invoiceNumber,
            orderId = layout.valueAfter("Order No.:"),
            invoiceDate = date,
            lines = lines,
            charges = charges,
            total = total.orFail("total"),
        )
    }

    private fun description(line: Line, columns: PageColumns): String =
        line.words.filter { it.x > columns.descriptionLeft && it.right < columns.descriptionRight }
            .joinToString(" ") { it.text }

    private data class PageColumns(val descriptionLeft: Float, val descriptionRight: Float, val headerBottom: Float)

    private fun pageColumns(layout: Layout, header: Line): PageColumns? {
        val pageLines = layout.lines.filter { it.page == header.page }
        val serial = pageLines.flatMap { it.words }.firstOrNull { it.text == "SR" } ?: return null
        val mrp = header.words.firstOrNull { it.text.startsWith("MRP") } ?: return null
        val headerBottom = pageLines.filter { it.y >= serial.y && it.y <= header.y + HEADER_DEPTH }
            .filter { line -> line.words.any { it.text == "Amt." } }
            .maxOfOrNull { it.y } ?: header.y
        return PageColumns(serial.right, mrp.x, headerBottom)
    }

    private fun isAnchor(line: Line, columns: PageColumns): Boolean {
        val first = line.words.firstOrNull() ?: return false
        return first.x < columns.descriptionLeft &&
            first.text.toIntOrNull() != null &&
            line.words.any { HSN.matches(it.text) }
    }

    private fun parseRow(row: TableRow): ParsedLine? {
        val fields = row.anchor.words.drop(1).map { it.text }
        val hsnIndex = fields.indexOfFirst { HSN.matches(it) }
        val units = fields.getOrNull(hsnIndex + 1)?.toIntOrNull()
        val parts = listOf(TAXABLE, CGST_AMOUNT, SGST_AMOUNT, CESS_AMOUNT).map { offset ->
            fields.getOrNull(hsnIndex + offset)?.let(::parseAmount)
        }
        return if (hsnIndex > 0 && units != null && parts.all { it != null }) {
            ParsedLine(
                rawName = row.description,
                units = units,
                amount = parts.filterNotNull().sum(),
                mrp = fields.getOrNull(hsnIndex - 1)?.let(::parseAmount),
                hsn = fields[hsnIndex],
            )
        } else {
            null
        }
    }

    private fun footer(lines: List<Line>): Pair<List<ParsedCharge>, Money?> {
        val charges = mutableListOf<ParsedCharge>()
        lines.forEach { line ->
            val amount = line.words.lastOrNull()?.let { parseAmount(it.text) } ?: return@forEach
            val label = line.words.dropLast(1).joinToString(" ") { it.text }
            if (label.startsWith("Invoice Value", ignoreCase = true)) return charges to amount
            if (label.isNotEmpty() &&
                !amount.isZero
            ) {
                charges += ParsedCharge(ChargeClassifier.classify(label, amount), label, amount)
            }
        }
        return charges to null
    }

    private companion object {
        val HSN = Regex("""^\d{8}$""")
        const val HEADER_DEPTH = 12f
        const val TAXABLE = 4
        const val CGST_AMOUNT = 7
        const val SGST_AMOUNT = 8
        const val CESS_AMOUNT = 9
    }
}
