package app.rasoi.feature.invoiceimport.upload

import app.rasoi.core.domain.bills.ImportInvoicesUseCase
import app.rasoi.core.domain.bills.InvoiceFile
import app.rasoi.feature.invoiceimport.BillError

/** The invoice PDFs chosen so far and whether they are being uploaded. */
data class ImportUiState(
    val files: List<InvoiceFile> = emptyList(),
    val isUploading: Boolean = false,
    val error: BillError? = null,
) {
    val canAddMore: Boolean get() = files.size < ImportInvoicesUseCase.MAX_FILES
    val canUpload: Boolean get() = files.isNotEmpty() && !isUploading
}
