package app.rasoi.server.parsing

import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Platform
import app.rasoi.server.parsing.extract.ExtractedDocument
import app.rasoi.server.parsing.model.ParsedInvoice

/** A template parser for one platform's invoice layout (design-plan 6.5). */
interface InvoiceParser {
    val platform: Platform

    /** True when [document] carries this platform's signatures; never decided by file name. */
    fun matches(document: ExtractedDocument): Boolean

    fun parse(document: ExtractedDocument): AppResult<ParsedInvoice>
}
