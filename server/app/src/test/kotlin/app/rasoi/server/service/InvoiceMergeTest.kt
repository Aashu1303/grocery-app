package app.rasoi.server.service

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.ImportConfidence
import app.rasoi.core.model.Money
import app.rasoi.core.model.Platform
import app.rasoi.server.parsing.Confidence
import app.rasoi.server.parsing.ParseResult
import app.rasoi.server.parsing.model.ParsedCharge
import app.rasoi.server.parsing.model.ParsedInvoice
import app.rasoi.server.parsing.model.ParsedLine
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class InvoiceMergeTest {
    private fun result(
        number: String,
        orderId: String? = "ORDER-1",
        platform: Platform = Platform.BLINKIT,
        day: Int = 4,
        confidence: Confidence = Confidence.HIGH,
        amount: Long = 10_000,
    ) = ParseResult(
        invoice = ParsedInvoice(
            platform = platform,
            invoiceNumber = number,
            orderId = orderId,
            invoiceDate = LocalDate(2026, 10, day),
            lines = listOf(ParsedLine("Item $number", 1, Money(amount))),
            charges = listOf(ParsedCharge(ChargeKind.HANDLING, "Fee $number", Money(500))),
            total = Money(amount + 500),
        ),
        warnings = emptyList(),
        confidence = confidence,
    )

    private fun violation(result: AppResult<*>): String =
        assertIs<AppError.Validation>(assertIs<AppResult.Failure>(result).error).violations.single().code

    @Test
    fun aSingleInvoiceIsKeptAsIs() {
        val merged =
            assertIs<AppResult.Success<InvoiceMerge.Merged>>(InvoiceMerge.merge(listOf(result("A", orderId = null))))
        assertEquals(result("A", orderId = null).invoice, merged.value.invoice)
        assertEquals(ImportConfidence.HIGH, merged.value.confidence)
        assertEquals(listOf("A"), merged.value.invoiceNumbers)
    }

    @Test
    fun invoicesOfOneOrderAreCombined() {
        val results = listOf(
            result("A", day = 5, amount = 10_000),
            result("B", day = 4, confidence = Confidence.MEDIUM, amount = 2_000),
        )
        val merged = assertIs<AppResult.Success<InvoiceMerge.Merged>>(InvoiceMerge.merge(results)).value
        assertEquals("A", merged.invoice.invoiceNumber)
        assertEquals(LocalDate(2026, 10, 4), merged.invoice.invoiceDate)
        assertEquals(listOf("Item A", "Item B"), merged.invoice.lines.map { it.rawName })
        assertEquals(listOf("Fee A", "Fee B"), merged.invoice.charges.map { it.label })
        assertEquals(Money(13_000), merged.invoice.total)
        assertEquals(ImportConfidence.MEDIUM, merged.confidence)
        assertEquals(listOf("A", "B"), merged.invoiceNumbers)
    }

    @Test
    fun rejectsMixedPlatformsOrdersAndRepeatedInvoices() {
        assertEquals(
            "different_orders",
            violation(InvoiceMerge.merge(listOf(result("A"), result("B", orderId = "ORDER-2")))),
        )
        assertEquals(
            "different_orders",
            violation(InvoiceMerge.merge(listOf(result("A"), result("B", platform = Platform.ZEPTO)))),
        )
        assertEquals(
            "different_orders",
            violation(InvoiceMerge.merge(listOf(result("A", orderId = null), result("B", orderId = null)))),
        )
        assertEquals("repeated_invoice", violation(InvoiceMerge.merge(listOf(result("A"), result("A")))))
    }
}
