package app.rasoi.server.parsing.template

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Platform
import app.rasoi.server.parsing.InvoiceParser
import app.rasoi.server.parsing.extract.ExtractedDocument
import app.rasoi.server.parsing.extract.ExtractedDocumentCodec
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Each template recognises only its own platform's fixtures and fails with a typed error on damaged input. */
class TemplateDetectionTest {
    private val root = File(requireNotNull(System.getProperty("rasoi.fixtures")) { "rasoi.fixtures not set" })
    private val parsers = listOf(
        InstamartInvoiceParser(),
        ZeptoInvoiceParser(),
        BlinkitInvoiceParser(),
        HandpickdInvoiceParser(),
    )

    private fun fixtures(): Map<Platform, ExtractedDocument> = root.walkTopDown()
        .filter { it.name.endsWith(".redacted.txt") }
        .associate { Platform.valueOf(it.parentFile.name.uppercase()) to ExtractedDocumentCodec.decode(it.readText()) }

    @Test
    fun everyParserMatchesExactlyItsOwnPlatform() {
        fixtures().forEach { (platform, document) ->
            assertEquals(listOf(platform), parsers.filter { it.matches(document) }.map { it.platform }, "$platform")
        }
    }

    @Test
    fun noParserMatchesAnUnrelatedDocument() {
        val receipt = ExtractedDocumentCodec.decode(
            "page 1 595 842\n40 40 60 8 Electricity\n40 60 30 8 Bill\n40 80 50 8 Total\n200 80 30 8 1200.00\n",
        )
        assertTrue(parsers.none { it.matches(receipt) })
    }

    @Test
    fun aDocumentMissingItsLowerHalfIsUnreadableNotACrash() {
        fixtures().forEach { (platform, document) ->
            val firstPageTop = ExtractedDocument(
                document.pages.take(1).map { page -> page.copy(words = page.words.filter { it.y < page.height / 4 }) },
            )
            val result = parserFor(platform).parse(firstPageTop)
            val error = assertIs<AppError.Validation>(assertIs<AppResult.Failure>(result, "$platform").error)
            assertTrue(error.violations.single().code.startsWith("unreadable_"), "$platform: $error")
        }
    }

    private fun parserFor(platform: Platform): InvoiceParser = parsers.single { it.platform == platform }
}
