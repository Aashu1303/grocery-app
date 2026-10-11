package app.rasoi.server.parsing

import app.rasoi.server.parsing.model.ParsedInvoice

/** Design-plan 6.5 step 5: tidy names and drop zero-value promo lines such as free flyers. */
object Normalizer {
    private val SPACES = Regex("\\s+")

    fun normalize(invoice: ParsedInvoice): ParsedInvoice = invoice.copy(
        lines = invoice.lines
            .filterNot { it.amount.isZero }
            .map { it.copy(rawName = it.rawName.replace(SPACES, " ").trim()) },
        charges = invoice.charges.filterNot { it.amount.isZero },
    )
}
