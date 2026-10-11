package app.rasoi.feature.invoiceimport.upload

import app.rasoi.core.domain.bills.InvoiceFile

sealed interface ImportUiEvent {
    data class FilesPicked(val files: List<InvoiceFile>) : ImportUiEvent

    data class RemoveFile(val index: Int) : ImportUiEvent

    data object Upload : ImportUiEvent
}
