package app.rasoi.server.parsing

import app.rasoi.core.model.sum
import app.rasoi.server.parsing.model.ParseWarning
import app.rasoi.server.parsing.model.ParsedInvoice

/** Design-plan 6.5 step 6: lines plus charges (discounts and round-off included) must equal the printed total. */
object Reconciler {
    fun check(invoice: ParsedInvoice): List<ParseWarning> {
        val computed = (invoice.lines.map { it.amount } + invoice.charges.map { it.amount }).sum()
        if (computed == invoice.total) return emptyList()
        return listOf(ParseWarning.TotalMismatch(computed, invoice.total))
    }
}
