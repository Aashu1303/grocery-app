package app.rasoi.server.parsing

import app.rasoi.core.model.AppResult
import app.rasoi.server.parsing.extract.ExtractedDocument
import app.rasoi.server.parsing.extract.PdfTextExtractor
import app.rasoi.server.parsing.testing.FixturePdf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Rendered fixtures parse exactly like the fixtures themselves, so server tests can upload them as PDFs. */
class FixturePdfRoundTripTest {
    private val pipeline = InvoicePipeline.default()

    @Test
    fun everyRenderedFixtureParsesToTheSameInvoice() {
        listOf("blinkit", "handpickd", "instamart", "zepto").forEach { platform ->
            val fixture = FixturePdf.fixture(platform)
            val extracted = assertIs<AppResult.Success<ExtractedDocument>>(
                PdfTextExtractor().extract(FixturePdf.render(fixture)),
            ).value
            val expected = assertIs<AppResult.Success<ParseResult>>(pipeline.parse(fixture), platform)
            assertEquals(expected, pipeline.parse(extracted), platform)
        }
    }
}
