package app.rasoi.server.parsing.extract

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Uses a synthetic PDF so no real invoice is needed. */
class PdfTextExtractorTest {
    private fun pdf(vararg pages: List<Triple<String, Float, Float>>): ByteArray = PDDocument().use { document ->
        pages.forEach { lines -> document.writePage(lines) }
        ByteArrayOutputStream().also(document::save).toByteArray()
    }

    private fun extracted(bytes: ByteArray): ExtractedDocument =
        assertIs<AppResult.Success<ExtractedDocument>>(PdfTextExtractor().extract(bytes)).value

    private fun failure(code: String) =
        AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("document", code))))

    private fun PDDocument.writePage(lines: List<Triple<String, Float, Float>>) {
        val font = PDType1Font(Standard14Fonts.FontName.HELVETICA)
        val page = PDPage(PDRectangle.A4).also(::addPage)
        PDPageContentStream(this, page).use { stream ->
            lines.forEach { (text, x, yFromTop) ->
                stream.beginText()
                stream.setFont(font, FONT_SIZE)
                stream.newLineAtOffset(x, PDRectangle.A4.height - yFromTop)
                stream.showText(text)
                stream.endText()
            }
        }
    }

    @Test
    fun extractsWordsWithPositionsOnEveryPage() {
        val bytes = pdf(
            listOf(Triple("Invoice No: INV-1", 40f, 100f), Triple("Bill Total Rs 130", 40f, 300f)),
            listOf(Triple("Page two", 60f, 50f)),
        )
        val document = extracted(bytes)
        assertEquals(2, document.pages.size)
        assertEquals(
            listOf("Invoice", "No:", "INV-1", "Bill", "Total", "Rs", "130"),
            document.pages[0].words.map { it.text },
        )
        val invoice = document.pages[0].words.first()
        assertEquals(40f, invoice.x, 0.5f)
        assertTrue(invoice.y in 90f..100f, "y=${invoice.y}")
        val total = document.pages[0].words.first { it.text == "Bill" }
        assertTrue(total.y > invoice.y)
        assertEquals(listOf("Page", "two"), document.pages[1].words.map { it.text })
    }

    @Test
    fun separatesWordsOnTheSameLineByGaps() {
        val bytes = pdf(listOf(Triple("Potato", 40f, 100f), Triple("26.00", 400f, 100f)))
        val words = extracted(bytes).words
        assertEquals(listOf("Potato", "26.00"), words.map { it.text })
        assertEquals(words[0].y, words[1].y, 0.5f)
    }

    @Test
    fun rejectsBytesThatAreNotAPdf() {
        assertEquals(failure("not_a_pdf"), PdfTextExtractor().extract("not a pdf".toByteArray()))
    }

    @Test
    fun rejectsPasswordProtectedPdfs() {
        val bytes = PDDocument().use { document ->
            document.writePage(listOf(Triple("Secret", 40f, 100f)))
            document.protect(StandardProtectionPolicy("owner", "user", AccessPermission()))
            ByteArrayOutputStream().also(document::save).toByteArray()
        }
        assertEquals(failure("encrypted"), PdfTextExtractor().extract(bytes))
    }

    @Test
    fun rejectsPdfsWithoutATextLayer() {
        assertEquals(failure("no_text"), PdfTextExtractor().extract(pdf(emptyList())))
    }

    private companion object {
        const val FONT_SIZE = 10f
    }
}
