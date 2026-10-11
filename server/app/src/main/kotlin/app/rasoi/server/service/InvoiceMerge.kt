package app.rasoi.server.service

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.ImportConfidence
import app.rasoi.core.model.sum
import app.rasoi.server.parsing.ParseResult
import app.rasoi.server.parsing.model.ParsedInvoice

/** Several PDFs of one order become one bill (design-plan section 2: fees may arrive as a separate invoice). */
internal object InvoiceMerge {
    data class Merged(val invoice: ParsedInvoice, val confidence: ImportConfidence, val invoiceNumbers: List<String>)

    fun merge(results: List<ParseResult>): AppResult<Merged> {
        val invoices = results.map { it.invoice }
        val first = invoices.first()
        val sameOrder = invoices.all { it.platform == first.platform } &&
            (invoices.size == 1 || (first.orderId != null && invoices.all { it.orderId == first.orderId }))
        if (!sameOrder) {
            return AppResult.Failure(
                AppError.Validation(listOf(AppError.FieldViolation("documents", "different_orders"))),
            )
        }
        val numbers = invoices.map { it.invoiceNumber }
        if (numbers.toSet().size != numbers.size) {
            return AppResult.Failure(
                AppError.Validation(listOf(AppError.FieldViolation("documents", "repeated_invoice"))),
            )
        }
        val merged = first.copy(
            invoiceDate = invoices.minOf { it.invoiceDate },
            lines = invoices.flatMap { it.lines },
            charges = invoices.flatMap { it.charges },
            total = invoices.map { it.total }.sum(),
        )
        val confidence = results.maxOf { ImportConfidence.valueOf(it.confidence.name) }
        return AppResult.Success(Merged(merged, confidence, numbers))
    }
}
