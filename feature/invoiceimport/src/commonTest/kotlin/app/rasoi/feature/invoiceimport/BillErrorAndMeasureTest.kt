package app.rasoi.feature.invoiceimport

import app.rasoi.core.model.AppError
import app.rasoi.core.model.Quantity
import app.rasoi.core.model.QuantityUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class BillErrorAndMeasureTest {
    private fun invalid(code: String) = AppError.Validation(listOf(AppError.FieldViolation("documents[0]", code)))

    @Test
    fun mapsTransportAndPermissionErrors() {
        assertEquals(BillError.OFFLINE, AppError.Offline.billError())
        assertEquals(BillError.OFFLINE, AppError.Timeout.billError())
        assertEquals(BillError.FORBIDDEN, AppError.Forbidden.billError())
        assertEquals(BillError.NOT_FOUND, AppError.NotFound("bill").billError())
        assertEquals(BillError.GENERIC, AppError.Server(500).billError())
    }

    @Test
    fun mapsConflicts() {
        assertEquals(BillError.DUPLICATE, AppError.Conflict("duplicate_invoice").billError())
        assertEquals(BillError.NOT_EDITABLE, AppError.Conflict("bill_not_editable").billError())
        assertEquals(BillError.GENERIC, AppError.Conflict("other").billError())
    }

    @Test
    fun mapsDocumentValidationCodes() {
        mapOf(
            "not_a_pdf" to BillError.NOT_A_PDF,
            "empty" to BillError.NOT_A_PDF,
            "encrypted" to BillError.ENCRYPTED,
            "no_text" to BillError.NO_TEXT,
            "unsupported_format" to BillError.UNSUPPORTED,
            "unreadable_total" to BillError.UNSUPPORTED,
            "different_orders" to BillError.DIFFERENT_ORDERS,
            "repeated_invoice" to BillError.REPEATED,
            "too_large" to BillError.TOO_LARGE,
            "count" to BillError.TOO_MANY,
            "invalid_base64" to BillError.GENERIC,
        ).forEach { (code, expected) -> assertEquals(expected, invalid(code).billError(), code) }
        assertEquals(BillError.GENERIC, AppError.Validation(emptyList()).billError())
    }

    @Test
    fun everyErrorHasAMessage() {
        assertEquals(BillError.entries.toSet(), billErrorMessages.keys)
    }

    @Test
    fun formatsMeasuresTheWayInvoicesPrintThem() {
        assertEquals("470 g", Quantity.grams(470).measureText())
        assertEquals("1.235 g", Quantity(1_235, QuantityUnit.MILLIGRAM).measureText())
        assertEquals("1.5 kg", Quantity.grams(1_500).measureText())
        assertEquals("2 kg", Quantity.kilograms(2).measureText())
        assertEquals("500 ml", Quantity.millilitres(500).measureText())
        assertEquals("1.5 L", Quantity.millilitres(1_500).measureText())
        assertEquals("6 pc", Quantity.count(6).measureText())
        assertEquals("0.5 pc", Quantity(500, QuantityUnit.COUNT).measureText())
    }
}
