package app.rasoi.server.parsing

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Money
import app.rasoi.core.model.Platform
import app.rasoi.server.parsing.extract.ExtractedDocument
import app.rasoi.server.parsing.extract.ExtractedPage
import app.rasoi.server.parsing.extract.TextWord
import app.rasoi.server.parsing.model.ParseWarning
import app.rasoi.server.parsing.model.ParsedInvoice
import app.rasoi.server.parsing.model.ParsedLine
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class InvoicePipelineTest {
    private val document =
        ExtractedDocument(listOf(ExtractedPage(1, 595f, 842f, listOf(TextWord("x", 0f, 0f, 1f, 1f)))))
    private val unsupported = AppError.Validation(listOf(AppError.FieldViolation("document", "unsupported_format")))

    private class FakeParser(
        override val platform: Platform,
        private val matches: Boolean,
        private val result: AppResult<ParsedInvoice> = AppResult.Success(invoice(platform = platform)),
    ) : InvoiceParser {
        var parsed = 0

        override fun matches(document: ExtractedDocument) = matches

        override fun parse(document: ExtractedDocument): AppResult<ParsedInvoice> = result.also { parsed++ }
    }

    private fun success(result: AppResult<ParseResult>): ParseResult =
        assertIs<AppResult.Success<ParseResult>>(result).value

    private fun pipelineReturning(parsed: ParsedInvoice) =
        InvoicePipeline(listOf(FakeParser(Platform.ZEPTO, true, AppResult.Success(parsed))))

    @Test
    fun usesTheFirstMatchingParserOnly() {
        val blinkit = FakeParser(Platform.BLINKIT, matches = true)
        val zepto = FakeParser(Platform.ZEPTO, matches = true)
        val pipeline = InvoicePipeline(listOf(FakeParser(Platform.INSTAMART, false), blinkit, zepto))
        val result = success(pipeline.parse(document))
        assertEquals(Platform.BLINKIT, result.invoice.platform)
        assertEquals(1 to 0, blinkit.parsed to zepto.parsed)
    }

    @Test
    fun reportsUnsupportedWhenNoTemplateMatches() {
        val result = InvoicePipeline(listOf(FakeParser(Platform.ZEPTO, matches = false))).parse(document)
        assertEquals(AppResult.Failure(unsupported), result)
    }

    @Test
    fun aReconciledTemplateParseIsHighConfidenceAndNormalized() {
        val parsed = invoice(
            lines = listOf(ParsedLine(" Onion  Red ", 1, Money(4_000)), ParsedLine("Flyer", 1, Money.ZERO)),
        )
        val result = success(pipelineReturning(parsed).parse(document))
        assertEquals(Confidence.HIGH, result.confidence)
        assertEquals(emptyList(), result.warnings)
        assertEquals(listOf("Onion Red"), result.invoice.lines.map { it.rawName })
    }

    @Test
    fun aTotalMismatchLowersConfidenceToMedium() {
        val parsed = invoice(total = Money(4_100))
        val result = success(pipelineReturning(parsed).parse(document))
        assertEquals(Confidence.MEDIUM, result.confidence)
        assertEquals(listOf<ParseWarning>(ParseWarning.TotalMismatch(Money(4_000), Money(4_100))), result.warnings)
    }

    @Test
    fun theFallbackParsesUnsupportedDocumentsWithLowConfidence() = runTest {
        val fallback = InvoiceFallback { AppResult.Success(invoice(platform = Platform.OTHER)) }
        val result = success(InvoicePipeline(emptyList(), fallback).parseWithFallback(document))
        assertEquals(Platform.OTHER, result.invoice.platform)
        assertEquals(Confidence.LOW, result.confidence)
        assertEquals(listOf<ParseWarning>(ParseWarning.ParsedByFallback), result.warnings)
    }

    @Test
    fun theFallbackAlsoCoversATemplateThatCannotReadItsDocument() = runTest {
        val broken = FakeParser(Platform.ZEPTO, matches = true, result = ParseFailures.unreadable("total"))
        val fallback = InvoiceFallback { AppResult.Success(invoice()) }
        val result = InvoicePipeline(listOf(broken), fallback).parseWithFallback(document)
        assertEquals(Confidence.LOW, success(result).confidence)
    }

    @Test
    fun theFallbackIsNotCalledWhenATemplateSucceeds() = runTest {
        var calls = 0
        val fallback = InvoiceFallback { AppResult.Success(invoice()).also { calls++ } }
        val result = InvoicePipeline(listOf(FakeParser(Platform.ZEPTO, true)), fallback).parseWithFallback(document)
        assertEquals(Confidence.HIGH, success(result).confidence)
        assertEquals(0, calls)
    }

    @Test
    fun withoutAFallbackTheTemplateFailureIsReturned() = runTest {
        assertEquals(AppResult.Failure(unsupported), InvoicePipeline(emptyList()).parseWithFallback(document))
    }

    @Test
    fun aFailingFallbackReturnsItsError() = runTest {
        val error = AppError.Timeout
        val result = InvoicePipeline(emptyList()) { AppResult.Failure(error) }.parseWithFallback(document)
        assertEquals(AppResult.Failure(error), result)
    }
}
