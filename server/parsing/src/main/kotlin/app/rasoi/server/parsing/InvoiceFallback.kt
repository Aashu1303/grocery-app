package app.rasoi.server.parsing

import app.rasoi.core.model.AppResult
import app.rasoi.server.parsing.extract.ExtractedDocument
import app.rasoi.server.parsing.model.ParsedInvoice

/** Design-plan 6.5 step 4: parses documents no template handles, for example with an LLM. */
fun interface InvoiceFallback {
    suspend fun parse(document: ExtractedDocument): AppResult<ParsedInvoice>
}
