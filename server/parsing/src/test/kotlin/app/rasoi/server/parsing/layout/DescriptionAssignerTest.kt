package app.rasoi.server.parsing.layout

import app.rasoi.server.parsing.extract.TextWord
import kotlin.test.Test
import kotlin.test.assertEquals

class DescriptionAssignerTest {
    /** A line is "#<text>" for an anchor carrying numbers, or plain wrapped description text. */
    private fun lines(vararg texts: String) = texts.mapIndexed { index, text ->
        Line(1, index * 10f, listOf(TextWord(text, 0f, index * 10f, 10f, 8f)))
    }

    private fun assign(alignment: CellAlignment, vararg texts: String) = DescriptionAssigner.assign(
        lines = lines(*texts),
        isAnchor = { it.text.startsWith("#") },
        description = { it.text.removePrefix("#") },
        alignment = alignment,
    ).map { it.description }

    @Test
    fun centredCellsSplitWrappedTextAroundEachAnchor() {
        assertEquals(
            listOf("Amul Taaza Milk", "Fresh Tomato Hybrid", "Onion"),
            assign(CellAlignment.CENTERED, "Amul", "#Taaza", "Milk", "Fresh", "#Tomato", "Hybrid", "#Onion"),
        )
    }

    @Test
    fun centredCellsGiveLeftoverTextAboveToTheNextItem() {
        assertEquals(
            listOf("Bread", "Brown Eggs Pack of 6"),
            assign(CellAlignment.CENTERED, "#Bread", "Brown", "#Eggs", "Pack of 6"),
        )
    }

    @Test
    fun topAlignedCellsKeepEveryFragmentWithTheAnchorAbove() {
        assertEquals(
            listOf("Potato Fresh 1 kg", "Banana"),
            assign(CellAlignment.TOP, "Header", "#Potato", "Fresh", "1 kg", "#Banana"),
        )
    }

    @Test
    fun skipsBlankDescriptionsAndReturnsNothingWithoutAnchors() {
        assertEquals(listOf("Rice"), assign(CellAlignment.TOP, "#Rice", " "))
        assertEquals(emptyList(), assign(CellAlignment.CENTERED, "only", "text"))
    }
}
