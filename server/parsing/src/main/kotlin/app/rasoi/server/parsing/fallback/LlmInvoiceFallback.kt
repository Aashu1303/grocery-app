package app.rasoi.server.parsing.fallback

import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Money
import app.rasoi.core.model.Platform
import app.rasoi.core.model.flatMap
import app.rasoi.server.parsing.InvoiceFallback
import app.rasoi.server.parsing.ParseFailures
import app.rasoi.server.parsing.extract.ExtractedDocument
import app.rasoi.server.parsing.layout.Layout
import app.rasoi.server.parsing.model.ParsedCharge
import app.rasoi.server.parsing.model.ParsedInvoice
import app.rasoi.server.parsing.model.ParsedLine
import app.rasoi.server.parsing.template.ChargeClassifier
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

/**
 * Design-plan 6.5 step 4: an LLM reads the item table of an invoice no template understands. Header fields are read
 * locally and only [LineItemExcerpt] text is sent. The answer is validated against [LlmInvoiceJson]; anything
 * missing or out of range makes the document unreadable rather than guessed.
 */
class LlmInvoiceFallback(private val client: ChatCompletionClient) : InvoiceFallback {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun parse(document: ExtractedDocument): AppResult<ParsedInvoice> {
        val lines = Layout(document).lines
        val header = InvoiceHeader.read(lines)
        val excerpt = LineItemExcerpt.of(lines)
        val missing = when {
            header.invoiceNumber == null -> "invoice_number"
            header.date == null -> "date"
            else -> "lines"
        }
        if (header.invoiceNumber == null || header.date == null || excerpt == null) {
            return ParseFailures.unreadable(missing)
        }
        return client.completeJson(PROMPT, excerpt).flatMap { answer ->
            val invoice = decode(answer)?.let { toInvoice(it, header.invoiceNumber, header.orderId, header.date) }
            invoice?.let { AppResult.Success(it) } ?: ParseFailures.unreadable("fallback")
        }
    }

    private fun decode(answer: String): LlmInvoiceJson? = try {
        json.decodeFromString(LlmInvoiceJson.serializer(), answer)
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun toInvoice(answer: LlmInvoiceJson, number: String, orderId: String?, date: LocalDate): ParsedInvoice? {
        val lines = answer.lines.mapNotNull(::toLine).takeIf { it.size == answer.lines.size && it.size in 1..MAX_LINES }
        val charges = answer.charges.mapNotNull(::toCharge)
            .takeIf { it.size == answer.charges.size && it.size <= MAX_LINES }
        val total = Money.parse(answer.total.trim())
        return if (lines == null || charges == null || total == null) {
            null
        } else {
            ParsedInvoice(Platform.OTHER, number, orderId, date, lines, charges, total)
        }
    }

    private fun toLine(line: LlmInvoiceJson.Line): ParsedLine? = Money.parse(line.amount.trim())
        ?.takeIf { line.name.isNotBlank() && line.name.length <= MAX_NAME && line.units in 1..MAX_UNITS }
        ?.let { ParsedLine(rawName = line.name, units = line.units, amount = it) }

    private fun toCharge(charge: LlmInvoiceJson.Charge): ParsedCharge? = Money.parse(charge.amount.trim())
        ?.takeIf { charge.label.isNotBlank() && charge.label.length <= MAX_NAME }
        ?.let { ParsedCharge(ChargeClassifier.classify(charge.label, it), charge.label, it) }

    private companion object {
        const val MAX_LINES = 200
        const val MAX_NAME = 200
        const val MAX_UNITS = 999
        val PROMPT = """
            You read the item table of an Indian grocery invoice. Reply with one JSON object only:
            {"lines":[{"name":"...","units":1,"amount":"0.00"}],"charges":[{"label":"...","amount":"0.00"}],"total":"0.00"}
            - lines: every purchased item. name as printed, units is the item count, amount is what was paid for the
              whole line after line discounts and including taxes.
            - charges: bill-level fees, discounts and round-off as printed; discounts are negative.
            - Skip subtotal rows and tax summaries. total is the final amount payable.
            - Amounts are decimal strings in rupees, for example "129.00" or "-10.00". Never invent values.
        """.trimIndent()
    }
}
