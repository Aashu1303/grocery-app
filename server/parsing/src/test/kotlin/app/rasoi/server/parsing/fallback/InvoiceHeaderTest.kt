package app.rasoi.server.parsing.fallback

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class InvoiceHeaderTest {
    @Test
    fun readsLabelledReferencesAndDates() {
        val header = InvoiceHeader.read(
            linesOf("Tax Invoice", "Invoice No.: FM-1001", "Order ID : 77001", "Invoice Date: 04/10/2026"),
        )
        assertEquals(InvoiceHeader("FM-1001", "77001", LocalDate(2026, 10, 4)), header)
    }

    @Test
    fun ignoresReferencesWithoutDigitsAndFallsBackToTheFirstDate() {
        val header = InvoiceHeader.read(linesOf("Invoice Number: PENDING", "Printed 02-10-2026", "Order OD-5"))
        assertEquals(InvoiceHeader(null, "OD-5", LocalDate(2026, 10, 2)), header)
    }

    @Test
    fun leavesFieldsEmptyWhenNothingMatches() {
        assertEquals(InvoiceHeader(null, null, null), InvoiceHeader.read(linesOf("Thank you", "Total 10.00")))
    }
}
