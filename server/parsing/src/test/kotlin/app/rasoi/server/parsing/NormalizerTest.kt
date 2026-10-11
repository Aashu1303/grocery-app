package app.rasoi.server.parsing

import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.Money
import app.rasoi.server.parsing.model.ParsedCharge
import app.rasoi.server.parsing.model.ParsedLine
import kotlin.test.Test
import kotlin.test.assertEquals

class NormalizerTest {
    @Test
    fun collapsesWhitespaceInNames() {
        val invoice = invoice(lines = listOf(ParsedLine("  Amul   Taaza\tMilk ", 1, Money(2_900))))
        assertEquals("Amul Taaza Milk", Normalizer.normalize(invoice).lines.single().rawName)
    }

    @Test
    fun dropsZeroValueLinesAndCharges() {
        val invoice = invoice(
            lines = listOf(ParsedLine("Free flyer", 1, Money.ZERO), ParsedLine("Onion", 1, Money(4_000))),
            charges = listOf(
                ParsedCharge(ChargeKind.DELIVERY, "Delivery", Money.ZERO),
                ParsedCharge(ChargeKind.HANDLING, "Handling", Money(500)),
            ),
        )
        val normalized = Normalizer.normalize(invoice)
        assertEquals(listOf("Onion"), normalized.lines.map { it.rawName })
        assertEquals(listOf("Handling"), normalized.charges.map { it.label })
    }
}
