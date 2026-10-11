package app.rasoi.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class QuantityTest {
    @Test
    fun factoriesUseBaseUnits() {
        assertEquals(Quantity(470_000, QuantityUnit.MILLIGRAM), Quantity.grams(470))
        assertEquals(Quantity(1_000_000, QuantityUnit.MILLIGRAM), Quantity.kilograms(1))
        assertEquals(Quantity(250, QuantityUnit.MILLILITRE), Quantity.millilitres(250))
        assertEquals(Quantity(1_000, QuantityUnit.MILLILITRE), Quantity.litres(1))
        assertEquals(Quantity(3_000, QuantityUnit.COUNT), Quantity.count(3))
    }

    @Test
    fun arithmeticRequiresSameUnit() {
        assertEquals(Quantity.grams(700), Quantity.grams(200) + Quantity.grams(500))
        assertEquals(Quantity.count(1), Quantity.count(3) - Quantity.count(2))
        assertFailsWith<IllegalArgumentException> { Quantity.grams(1) + Quantity.count(1) }
        assertFailsWith<IllegalArgumentException> { Quantity.grams(1) - Quantity.millilitres(1) }
    }

    @Test
    fun comparisonRequiresSameUnit() {
        assertTrue(Quantity.grams(2) > Quantity.grams(1))
        assertFailsWith<IllegalArgumentException> { Quantity.grams(2) > Quantity.count(1) }
    }

    @Test
    fun negativeAmountsAreRejected() {
        assertFailsWith<IllegalArgumentException> { Quantity(-1, QuantityUnit.COUNT) }
        assertFailsWith<IllegalArgumentException> { Quantity.count(1) - Quantity.count(2) }
    }

    @Test
    fun zeroIsRecognised() {
        assertTrue(Quantity.zero(QuantityUnit.MILLIGRAM).isZero)
    }
}
