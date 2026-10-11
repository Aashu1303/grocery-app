package app.rasoi.feature.invoiceimport

import app.rasoi.core.model.AppError
import kotlin.test.Test
import kotlin.test.assertEquals

class BillErrorTest {
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
}
