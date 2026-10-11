package app.rasoi.server.support

import app.rasoi.core.contract.bills.InvoiceDocumentDto
import app.rasoi.core.contract.bills.UploadInvoicesRequestDto
import app.rasoi.server.parsing.extract.ExtractedDocument
import app.rasoi.server.parsing.testing.FixturePdf
import java.util.Base64

/** Invoice PDFs rendered from the redacted fixtures, optionally with words swapped to make variants. */
object Invoices {
    fun pdf(platform: String, vararg replacements: Pair<String, String>): ByteArray =
        FixturePdf.render(FixturePdf.fixture(platform).replacing(replacements.toMap()))

    fun upload(vararg pdfs: ByteArray) = UploadInvoicesRequestDto(
        pdfs.mapIndexed { index, bytes ->
            InvoiceDocumentDto("invoice-$index.pdf", Base64.getEncoder().encodeToString(bytes))
        },
    )

    private fun ExtractedDocument.replacing(words: Map<String, String>) = copy(
        pages = pages.map { page ->
            page.copy(words = page.words.map { word -> words[word.text]?.let { word.copy(text = it) } ?: word })
        },
    )
}
