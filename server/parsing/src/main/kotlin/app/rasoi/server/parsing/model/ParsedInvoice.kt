package app.rasoi.server.parsing.model

import app.rasoi.core.model.Money
import app.rasoi.core.model.Platform
import kotlinx.datetime.LocalDate

/** The canonical result of parsing one invoice, before categorisation and allocation (design-plan 6.5). */
data class ParsedInvoice(
    val platform: Platform,
    val invoiceNumber: String,
    val orderId: String?,
    val invoiceDate: LocalDate,
    val lines: List<ParsedLine>,
    val charges: List<ParsedCharge>,
    val total: Money,
)
