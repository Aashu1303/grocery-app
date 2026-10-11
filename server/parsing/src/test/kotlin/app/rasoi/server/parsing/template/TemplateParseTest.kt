package app.rasoi.server.parsing.template

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Money
import app.rasoi.core.model.Quantity
import app.rasoi.core.model.QuantityUnit
import app.rasoi.server.parsing.invoice
import app.rasoi.server.parsing.model.ParsedInvoice
import kotlin.test.Test
import kotlin.test.assertEquals

class TemplateParseTest {
    private fun unreadable(what: String): AppResult<ParsedInvoice> =
        AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("document", "unreadable_$what"))))

    @Test
    fun returnsTheInvoiceTheTemplateBuilds() {
        val parsed = invoice()
        assertEquals(AppResult.Success(parsed), parsing { parsed })
    }

    @Test
    fun turnsAMissingFieldIntoATypedFailure() {
        val absent: String? = null
        assertEquals(unreadable("date"), parsing { absent.orFail("date").let { invoice() } })
    }

    @Test
    fun anInvoiceWithoutLinesIsUnreadable() {
        assertEquals(unreadable("lines"), parsing { invoice(lines = emptyList()) })
    }

    @Test
    fun valuesTheModelRejectsAreUnreadableNotCrashes() {
        assertEquals(unreadable("values"), parsing { invoice(total = Money(Long.MAX_VALUE) + Money(1)) })
        assertEquals(unreadable("values"), parsing { Quantity(-1, QuantityUnit.COUNT).let { invoice() } })
    }
}
