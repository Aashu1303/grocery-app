package app.rasoi.server.parsing.extract

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExtractedDocumentCodecTest {
    private val document = ExtractedDocument(
        listOf(
            ExtractedPage(
                1,
                595f,
                842f,
                listOf(TextWord("Bill", 36f, 120.5f, 20f, 7f), TextWord("Total", 58f, 120.5f, 25f, 7f)),
            ),
            ExtractedPage(2, 595f, 842f, listOf(TextWord("Rs 130", 386.67f, 40f, 30f, 7f))),
        ),
    )

    @Test
    fun roundTripsPagesAndWordsWithSpaces() {
        assertEquals(document, ExtractedDocumentCodec.decode(ExtractedDocumentCodec.encode(document)))
    }

    @Test
    fun ignoresCommentsAndBlankLines() {
        val decoded = ExtractedDocumentCodec.decode("# note\n\npage 1 10.00 20.00\n1.00 2.00 3.00 4.00 word\n")
        assertEquals(listOf("word"), decoded.words.map { it.text })
    }

    @Test
    fun rejectsMalformedWordLines() {
        assertFailsWith<IllegalArgumentException> { ExtractedDocumentCodec.decode("page 1 1 1\n1.0 2.0 broken") }
    }
}
