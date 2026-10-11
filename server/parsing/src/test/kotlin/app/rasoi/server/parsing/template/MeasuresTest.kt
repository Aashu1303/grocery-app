package app.rasoi.server.parsing.template

import app.rasoi.core.model.Quantity
import app.rasoi.core.model.QuantityUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MeasuresTest {
    @Test
    fun convertsWeightsToMilligrams() {
        assertEquals(Quantity.grams(470), parseMeasure("470.00", "g"))
        assertEquals(Quantity.grams(250), parseMeasure("250", "gms"))
        assertEquals(Quantity(1_500_000, QuantityUnit.MILLIGRAM), parseMeasure("1.5", "kg"))
        assertEquals(Quantity(1_235, QuantityUnit.MILLIGRAM), parseMeasure("1.235", "g"))
    }

    @Test
    fun convertsVolumesToMillilitres() {
        assertEquals(Quantity.millilitres(500), parseMeasure("500", "ml"))
        assertEquals(Quantity.millilitres(1_500), parseMeasure("1.5", "L"))
        assertEquals(Quantity.litres(2), parseMeasure("2", "ltr"))
    }

    @Test
    fun keepsCountsInThousandths() {
        assertEquals(Quantity.count(2), parseMeasure("2", "pc"))
        assertEquals(Quantity.count(6), parseMeasure("6", "Pieces"))
        assertEquals(Quantity(500, QuantityUnit.COUNT), parseMeasure("0.5", "pcs."))
    }

    @Test
    fun rejectsUnknownUnitsAndMalformedNumbers() {
        assertNull(parseMeasure("2", "dozen"))
        assertNull(parseMeasure("1.2345", "kg"))
        assertNull(parseMeasure("one", "kg"))
        assertNull(parseMeasure("-1", "kg"))
    }
}
