package app.rasoi.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class QuantityFormatTest {
    @Test
    fun formatsMeasuresTheWayInvoicesPrintThem() {
        assertEquals("470 g", Quantity.grams(470).format())
        assertEquals("1.235 g", Quantity(1_235, QuantityUnit.MILLIGRAM).format())
        assertEquals("1.5 kg", Quantity.grams(1_500).format())
        assertEquals("2 kg", Quantity.kilograms(2).format())
        assertEquals("500 ml", Quantity.millilitres(500).format())
        assertEquals("1.5 L", Quantity.millilitres(1_500).format())
        assertEquals("6 pc", Quantity.count(6).format())
        assertEquals("0.5 pc", Quantity(500, QuantityUnit.COUNT).format())
    }
}
