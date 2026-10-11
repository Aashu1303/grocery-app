package app.rasoi.server.parsing.testing

import app.rasoi.server.parsing.extract.ExtractedDocument
import app.rasoi.server.parsing.extract.ExtractedDocumentCodec
import app.rasoi.server.parsing.extract.ExtractedPage
import app.rasoi.server.parsing.extract.TextWord
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Renders a redacted fixture back into a PDF, each word at its recorded position and width, so server and demo tests
 * can upload realistic invoices without any real PDF in the repository (ADR 0009).
 */
object FixturePdf {
    private const val HUNDRED_PERCENT = 100f
    private const val UNITS_PER_EM = 1000f

    /** Glyph box height of Helvetica capitals as a share of the font size, as PDFBox reports it. */
    private const val HEIGHT_PER_POINT = 0.718f

    fun render(document: ExtractedDocument): ByteArray = PDDocument().use { pdf ->
        val font = PDType1Font(Standard14Fonts.FontName.HELVETICA)
        document.pages.forEach { page -> pdf.addPage(page, font) }
        ByteArrayOutputStream().also(pdf::save).toByteArray()
    }

    private fun PDDocument.addPage(page: ExtractedPage, font: PDType1Font) {
        val pdPage = PDPage(PDRectangle(page.width, page.height)).also(::addPage)
        PDPageContentStream(this, pdPage).use { stream ->
            page.words.forEach { word -> stream.write(font, word, page.height) }
        }
    }

    /** The redacted fixture `<platform>/<case>.redacted.txt` under the `rasoi.fixtures` directory. */
    fun fixture(platform: String, case: String = "order-1"): ExtractedDocument {
        val root = requireNotNull(System.getProperty("rasoi.fixtures")) { "rasoi.fixtures not set" }
        return ExtractedDocumentCodec.decode(File(root, "$platform/$case.redacted.txt").readText())
    }

    private fun PDPageContentStream.write(font: PDType1Font, word: TextWord, pageHeight: Float) {
        val size = word.height / HEIGHT_PER_POINT
        val naturalWidth = font.getStringWidth(word.text) / UNITS_PER_EM * size
        beginText()
        setFont(font, size)
        if (naturalWidth > 0f) setHorizontalScaling(word.width / naturalWidth * HUNDRED_PERCENT)
        newLineAtOffset(word.x, pageHeight - word.y - word.height)
        showText(word.text)
        endText()
    }
}
