package app.rasoi.server.parsing

import app.rasoi.core.model.AppResult
import app.rasoi.core.model.map
import app.rasoi.server.parsing.extract.ExtractedDocument
import app.rasoi.server.parsing.model.ParseWarning
import app.rasoi.server.parsing.model.ParsedInvoice
import app.rasoi.server.parsing.template.BlinkitInvoiceParser
import app.rasoi.server.parsing.template.FirstclubInvoiceParser
import app.rasoi.server.parsing.template.HandpickdInvoiceParser
import app.rasoi.server.parsing.template.InstamartInvoiceParser
import app.rasoi.server.parsing.template.ZeptoInvoiceParser

/** Detect → template parse → normalize → reconcile (design-plan 6.5). The fallback runs when no template fits. */
class InvoicePipeline(private val parsers: List<InvoiceParser>, private val fallback: InvoiceFallback? = null) {
    /** Template-only parsing, for callers that cannot suspend. */
    fun parse(document: ExtractedDocument): AppResult<ParseResult> {
        val parser = parsers.firstOrNull { it.matches(document) } ?: return ParseFailures.unsupported()
        return parser.parse(document).map { finish(it, fromFallback = false) }
    }

    suspend fun parseWithFallback(document: ExtractedDocument): AppResult<ParseResult> {
        val template = parse(document)
        val backup = fallback ?: return template
        if (template is AppResult.Success) return template
        return backup.parse(document).map { finish(it, fromFallback = true) }
    }

    private fun finish(parsed: ParsedInvoice, fromFallback: Boolean): ParseResult {
        val invoice = Normalizer.normalize(parsed)
        val warnings = Reconciler.check(invoice) + listOfNotNull(ParseWarning.ParsedByFallback.takeIf { fromFallback })
        val confidence = when {
            fromFallback -> Confidence.LOW
            warnings.isNotEmpty() -> Confidence.MEDIUM
            else -> Confidence.HIGH
        }
        return ParseResult(invoice, warnings, confidence)
    }

    companion object {
        fun default(fallback: InvoiceFallback? = null) = InvoicePipeline(
            listOf(
                InstamartInvoiceParser(),
                ZeptoInvoiceParser(),
                BlinkitInvoiceParser(),
                HandpickdInvoiceParser(),
                FirstclubInvoiceParser(),
            ),
            fallback,
        )
    }
}
