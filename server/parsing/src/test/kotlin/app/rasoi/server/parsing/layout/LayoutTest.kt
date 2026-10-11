package app.rasoi.server.parsing.layout

import app.rasoi.server.parsing.extract.ExtractedDocument
import app.rasoi.server.parsing.extract.ExtractedPage
import app.rasoi.server.parsing.extract.TextWord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LayoutTest {
    private fun word(text: String, x: Float, y: Float, height: Float = 8f) = TextWord(text, x, y, 20f, height)

    private fun layout(vararg pages: List<TextWord>) = Layout(
        ExtractedDocument(pages.mapIndexed { index, words -> ExtractedPage(index + 1, 595f, 842f, words) }),
    )

    @Test
    fun groupsWordsWithinHalfAGlyphIntoLeftToRightLines() {
        val layout = layout(
            listOf(
                word("Total", 300f, 101f),
                word("Bill", 40f, 100f),
                word("Next", 40f, 120f),
                word("130", 500f, 103f),
            ),
        )
        assertEquals(listOf("Bill Total 130", "Next"), layout.lines.map { it.text })
        assertEquals(100f, layout.lines.first().y)
    }

    @Test
    fun neverMergesLinesAcrossPages() {
        val layout = layout(listOf(word("One", 40f, 100f)), listOf(word("Two", 40f, 100f)))
        assertEquals(listOf(1 to "One", 2 to "Two"), layout.lines.map { it.page to it.text })
    }

    @Test
    fun findsPhrasesIgnoringCase() {
        val layout = layout(listOf(word("Invoice", 40f, 50f), word("Value", 80f, 50f)))
        assertEquals("Invoice Value", layout.find("invoice value")?.text)
        assertNull(layout.find("Grand Total"))
    }

    @Test
    fun readsTheValueAfterAMultiWordLabelSkippingAColon() {
        val layout = layout(
            listOf(word("Invoice", 40f, 50f), word("No", 80f, 50f), word(":", 100f, 50f), word("INV-1", 120f, 50f)),
        )
        assertEquals("INV-1", layout.valueAfter("invoice no"))
        assertNull(layout.valueAfter("Order No"))
    }

    @Test
    fun returnsNullWhenTheLabelEndsTheLine() {
        assertNull(layout(listOf(word("Invoice", 40f, 50f), word("No", 80f, 50f))).valueAfter("Invoice No"))
    }
}
