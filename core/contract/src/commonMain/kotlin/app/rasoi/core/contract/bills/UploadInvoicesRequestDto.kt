package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

/** One or more PDFs of the same order, parsed into one draft bill. */
@Serializable
data class UploadInvoicesRequestDto(val documents: List<InvoiceDocumentDto>)
