package app.rasoi.server.parsing.extract

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import java.io.IOException

/**
 * Extracts words with positions from every page of a PDF (design-plan 6.5, step 1). Uploads that are not PDFs, are
 * password-protected or have no text layer (scans) fail with a typed `document` validation error.
 */
class PdfTextExtractor {
    fun extract(pdf: ByteArray): AppResult<ExtractedDocument> {
        val document = try {
            Loader.loadPDF(pdf).use { document ->
                ExtractedDocument((1..document.numberOfPages).map { number -> extractPage(document, number) })
            }
        } catch (_: InvalidPasswordException) {
            return invalid("encrypted")
        } catch (_: IOException) {
            return invalid("not_a_pdf")
        }
        return if (document.words.isEmpty()) invalid("no_text") else AppResult.Success(document)
    }

    private fun invalid(code: String): AppResult<Nothing> =
        AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("document", code))))

    private fun extractPage(document: PDDocument, number: Int): ExtractedPage {
        val collector = WordCollector().apply {
            startPage = number
            endPage = number
            sortByPosition = true
        }
        collector.getText(document)
        val box = document.getPage(number - 1).mediaBox
        return ExtractedPage(number, box.width, box.height, collector.words)
    }

    private class WordCollector : PDFTextStripper() {
        val words = mutableListOf<TextWord>()

        override fun writeString(text: String, textPositions: List<TextPosition>) {
            var current = mutableListOf<TextPosition>()
            for (position in textPositions) {
                val gap = current.lastOrNull()?.let { position.xDirAdj - (it.xDirAdj + it.widthDirAdj) } ?: 0f
                if (position.unicode.isBlank() || gap > position.widthOfSpace * SPACE_FACTOR) {
                    flush(current)
                    current = mutableListOf()
                }
                if (position.unicode.isNotBlank()) current += position
            }
            flush(current)
        }

        private fun flush(glyphs: List<TextPosition>) {
            if (glyphs.isEmpty()) return
            val first = glyphs.first()
            val last = glyphs.last()
            val height = glyphs.maxOf { it.heightDir }
            words += TextWord(
                text = glyphs.joinToString("") { it.unicode },
                x = first.xDirAdj,
                y = first.yDirAdj - height,
                width = last.xDirAdj + last.widthDirAdj - first.xDirAdj,
                height = height,
            )
        }

        private companion object {
            const val SPACE_FACTOR = 0.5f
        }
    }
}
