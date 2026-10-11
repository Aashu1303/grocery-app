package app.rasoi.server.parsing

import app.rasoi.server.parsing.model.ParseWarning
import app.rasoi.server.parsing.model.ParsedInvoice

data class ParseResult(val invoice: ParsedInvoice, val warnings: List<ParseWarning>, val confidence: Confidence)
