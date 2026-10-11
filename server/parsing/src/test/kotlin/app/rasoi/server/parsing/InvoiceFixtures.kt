package app.rasoi.server.parsing

import app.rasoi.core.model.Money
import app.rasoi.core.model.Platform
import app.rasoi.core.model.sum
import app.rasoi.server.parsing.model.ParsedCharge
import app.rasoi.server.parsing.model.ParsedInvoice
import app.rasoi.server.parsing.model.ParsedLine
import kotlinx.datetime.LocalDate

/** A reconciled invoice unless [total] says otherwise. */
fun invoice(
    lines: List<ParsedLine> = listOf(ParsedLine("Onion", 1, Money(4_000))),
    charges: List<ParsedCharge> = emptyList(),
    total: Money = (lines.map { it.amount } + charges.map { it.amount }).sum(),
    platform: Platform = Platform.ZEPTO,
) = ParsedInvoice(platform, "INV-1", "ORDER-1", LocalDate(2026, 10, 4), lines, charges, total)
