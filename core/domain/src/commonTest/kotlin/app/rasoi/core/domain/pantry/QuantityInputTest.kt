package app.rasoi.core.domain.pantry

import app.rasoi.core.model.Quantity
import app.rasoi.core.model.QuantityUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QuantityInputTest {
    @Test
    fun convertsWholeAndFractionalAmountsToBaseUnits() {
        assertEquals(Quantity.count(2), QuantityInput.parse("2", InputUnit.PIECES))
        assertEquals(Quantity(500, QuantityUnit.COUNT), QuantityInput.parse("0.5", InputUnit.PIECES))
        assertEquals(Quantity.grams(250), QuantityInput.parse("250", InputUnit.GRAMS))
        assertEquals(Quantity(1_500, QuantityUnit.MILLIGRAM), QuantityInput.parse("1.5", InputUnit.GRAMS))
        assertEquals(Quantity.kilograms(2), QuantityInput.parse("2", InputUnit.KILOGRAMS))
        assertEquals(Quantity.grams(1_500), QuantityInput.parse("1.5", InputUnit.KILOGRAMS))
        assertEquals(Quantity.millilitres(500), QuantityInput.parse("500", InputUnit.MILLILITRES))
        assertEquals(Quantity.millilitres(1_500), QuantityInput.parse("1.5", InputUnit.LITRES))
    }

    @Test
    fun acceptsSurroundingSpacesAndACommaDecimalSeparator() {
        assertEquals(Quantity.grams(1_500), QuantityInput.parse(" 1,5 ", InputUnit.KILOGRAMS))
    }

    @Test
    fun rejectsTextThatIsNotAPositiveAmount() {
        listOf("", " ", "abc", "-1", "1.2.3", "1e3", "0", "0.000").forEach {
            assertNull(QuantityInput.parse(it, InputUnit.GRAMS), it)
        }
    }

    @Test
    fun rejectsPrecisionTheBaseUnitCannotHold() {
        assertNull(QuantityInput.parse("0.5", InputUnit.MILLILITRES))
        assertNull(QuantityInput.parse("1.2345", InputUnit.GRAMS))
        assertNull(QuantityInput.parse("0.0000001", InputUnit.KILOGRAMS))
        assertEquals(Quantity(100, QuantityUnit.MILLIGRAM), QuantityInput.parse("0.0001", InputUnit.KILOGRAMS))
    }

    @Test
    fun rejectsAmountsThatWouldOverflow() {
        assertNull(QuantityInput.parse("9999999999999999999", InputUnit.KILOGRAMS))
    }

    @Test
    fun everyInputUnitMapsToABaseUnit() {
        assertEquals(
            mapOf(
                InputUnit.PIECES to QuantityUnit.COUNT,
                InputUnit.GRAMS to QuantityUnit.MILLIGRAM,
                InputUnit.KILOGRAMS to QuantityUnit.MILLIGRAM,
                InputUnit.MILLILITRES to QuantityUnit.MILLILITRE,
                InputUnit.LITRES to QuantityUnit.MILLILITRE,
            ),
            InputUnit.entries.associateWith { it.base },
        )
    }
}
