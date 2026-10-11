package app.rasoi.feature.invoiceimport.picker

import androidx.compose.runtime.Composable
import app.rasoi.core.domain.bills.InvoiceFile

/** Desktop is only used for screenshots and tests, so picking does nothing. */
@Composable
actual fun rememberPdfPicker(onPicked: (List<InvoiceFile>) -> Unit): PdfPicker = PdfPicker {}
