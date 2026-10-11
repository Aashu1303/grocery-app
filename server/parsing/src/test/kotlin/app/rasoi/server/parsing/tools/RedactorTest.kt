package app.rasoi.server.parsing.tools

import app.rasoi.server.parsing.extract.ExtractedDocument
import app.rasoi.server.parsing.extract.ExtractedPage
import app.rasoi.server.parsing.extract.TextWord
import kotlin.test.Test
import kotlin.test.assertEquals

class RedactorTest {
    private val document = ExtractedDocument(
        listOf(
            ExtractedPage(
                1,
                595f,
                842f,
                listOf(
                    TextWord("Customer:", 10f, 10f, 40f, 8f),
                    TextWord("Asha", 55f, 10f, 20f, 8f),
                    TextWord("Rao", 80f, 10f, 20f, 8f),
                    TextWord("Order:", 10f, 30f, 30f, 8f),
                    TextWord("OD-REAL42", 45f, 30f, 50f, 8f),
                ),
            ),
        ),
    )

    @Test
    fun replacesSingleWordsAndMultiWordRunsSpanningTheirWidth() {
        val redacted = Redactor(listOf("Asha Rao" to "CUSTOMER NAME", "OD-REAL42" to "OD-DEMO0001")).redact(document)
        assertEquals(listOf("Customer:", "CUSTOMER_NAME", "Order:", "OD-DEMO0001"), redacted.words.map { it.text })
        val name = redacted.words[1]
        assertEquals(55f to 45f, name.x to name.width)
    }

    @Test
    fun leavesPartialRunsAlone() {
        val redacted = Redactor(listOf("Asha Kumar" to "X")).redact(document)
        assertEquals(document, redacted)
    }

    @Test
    fun reportsValuesThatSurviveRedactionIgnoringCase() {
        val partly = Redactor(listOf("OD-REAL42" to "OD-DEMO0001")).redact(document)
        assertEquals(listOf("asha rao"), Redactor.leaks(partly, listOf("asha rao", "OD-REAL42")))
        assertEquals(
            emptyList(),
            Redactor.leaks(Redactor(listOf("Asha Rao" to "C")).redact(partly), listOf("Asha Rao")),
        )
    }
}
