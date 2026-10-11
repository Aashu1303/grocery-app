package app.rasoi.server.parsing.fallback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LineItemExcerptTest {
    private val invoice = arrayOf(
        "Fresh Mart Tax Invoice",
        "Customer Name: Asha Rao",
        "Address: 12 Lake Road Pune",
        "Invoice No: FM-1001 Date: 04/10/2026",
        "Item Qty Rate Amount",
        "Tomato Hybrid 1 40.00 40.00",
        "500 g",
        "Paneer 1 90.00 90.00",
        "Call 9876543210 or help@freshmart.in for returns 0.00",
        "Delivery fee 20.00",
        "Total 150.00",
        "Thank you for shopping",
    )

    @Test
    fun keepsOnlyTheTableFromTheFirstLineWithTwoAmountsToTheLastAmount() {
        assertEquals(
            listOf(
                "Tomato Hybrid 1 40.00 40.00",
                "500 g",
                "Paneer 1 90.00 90.00",
                "Call [redacted] or [redacted] for returns 0.00",
                "Delivery fee 20.00",
                "Total 150.00",
            ),
            LineItemExcerpt.of(linesOf(*invoice))?.lines(),
        )
    }

    @Test
    fun dropsTableLinesThatCarryPersonalLabels() {
        val excerpt = LineItemExcerpt.of(linesOf("Rice 1 60.00 60.00", "Ship to Asha 0.00", "Total 60.00"))
        assertEquals("Rice 1 60.00 60.00\nTotal 60.00", excerpt)
    }

    @Test
    fun countsAmountsWrittenAfterACurrencySign() {
        assertEquals(
            "Onion Rs 30 Rs 30\nBill Total Rs 30",
            LineItemExcerpt.of(linesOf("Onion Rs 30 Rs 30", "Bill Total Rs 30")),
        )
    }

    @Test
    fun returnsNullWithoutAnItemTableOrWhenTooLong() {
        assertNull(LineItemExcerpt.of(linesOf("Hello", "Total 10.00")))
        val long = Array(400) { "Item number $it with a long description 1 10.00 10.00" }
        assertNull(LineItemExcerpt.of(linesOf(*long)))
    }
}
