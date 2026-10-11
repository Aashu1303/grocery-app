package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

/** One invoice PDF, Base64-encoded so the upload is ordinary idempotent JSON (ADR 0018). */
@Serializable
data class InvoiceDocumentDto(val fileName: String, val contentBase64: String)
