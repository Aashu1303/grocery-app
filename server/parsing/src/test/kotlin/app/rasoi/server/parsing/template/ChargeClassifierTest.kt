package app.rasoi.server.parsing.template

import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals

class ChargeClassifierTest {
    private fun classify(label: String, paise: Long = 1_000) = ChargeClassifier.classify(label, Money(paise))

    @Test
    fun recognisesChargeLabels() {
        assertEquals(ChargeKind.HANDLING, classify("Handling Charge"))
        assertEquals(ChargeKind.DELIVERY, classify("Delivery Fee"))
        assertEquals(ChargeKind.ROUND_OFF, classify("Round Off", paise = 2))
        assertEquals(ChargeKind.DISCOUNT, classify("Promo Discount"))
        assertEquals(ChargeKind.OTHER, classify("Small Cart Fee"))
    }

    @Test
    fun treatsAnyNegativeAmountAsADiscount() {
        assertEquals(ChargeKind.DISCOUNT, classify("Delivery Fee", paise = -3_000))
        assertEquals(ChargeKind.DISCOUNT, classify("Round Off", paise = -12))
    }
}
