package app.rasoi.feature.invoiceimport.picker

import androidx.compose.runtime.Composable
import app.rasoi.core.domain.bills.InvoiceFile

/** Opens the platform's file chooser for PDFs. */
fun interface PdfPicker {
    fun pick()
}

/** Remembers a [PdfPicker] that reports the chosen PDFs, read into memory, to [onPicked] on the main thread. */
@Composable
expect fun rememberPdfPicker(onPicked: (List<InvoiceFile>) -> Unit): PdfPicker
