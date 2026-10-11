package app.rasoi.server.parsing.template

import app.rasoi.core.model.AppResult
import app.rasoi.server.parsing.ParseFailures
import app.rasoi.server.parsing.model.ParsedInvoice

/** Signals a missing required field inside a template; converted to an [AppResult] before leaving the parser. */
internal class MissingFieldException(val field: String) : Exception("Missing $field")

/** Scope for template code: [orFail] turns an absent value into a typed "unreadable field" failure. */
internal object TemplateScope {
    fun <T : Any> T?.orFail(field: String): T = this ?: throw MissingFieldException(field)
}

/** Runs a template; an invoice without any item line is treated as unreadable too. */
internal inline fun parsing(block: TemplateScope.() -> ParsedInvoice): AppResult<ParsedInvoice> = try {
    AppResult.Success(TemplateScope.block().also { if (it.lines.isEmpty()) throw MissingFieldException("lines") })
} catch (missing: MissingFieldException) {
    ParseFailures.unreadable(missing.field)
} catch (_: ArithmeticException) {
    ParseFailures.unreadable("values") // amounts too large for exact arithmetic
} catch (_: IllegalArgumentException) {
    ParseFailures.unreadable("values") // a value the model rejects, such as a negative quantity
}
