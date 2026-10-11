package app.rasoi.server.parsing

import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.Money
import app.rasoi.server.parsing.model.ParseWarning
import app.rasoi.server.parsing.model.ParsedCharge
import app.rasoi.server.parsing.model.ParsedLine
import kotlin.test.Test
import kotlin.test.assertEquals

class ReconcilerTest {
    private val lines = listOf(ParsedLine("Milk", 1, Money(2_900)), ParsedLine("Bread", 2, Money(9_000)))
    private val charges = listOf(
        ParsedCharge(ChargeKind.HANDLING, "Handling", Money(1_100)),
        ParsedCharge(ChargeKind.DISCOUNT, "Promo", Money(-2_000)),
        ParsedCharge(ChargeKind.ROUND_OFF, "Round off", Money.ZERO),
    )

    @Test
    fun acceptsLinesPlusChargesThatEqualTheTotal() {
        assertEquals(emptyList(), Reconciler.check(invoice(lines, charges, total = Money(11_000))))
    }

    @Test
    fun reportsTheComputedAndPrintedTotalsOnMismatch() {
        assertEquals(
            listOf(ParseWarning.TotalMismatch(computed = Money(11_000), printed = Money(11_001))),
            Reconciler.check(invoice(lines, charges, total = Money(11_001))),
        )
    }
}
