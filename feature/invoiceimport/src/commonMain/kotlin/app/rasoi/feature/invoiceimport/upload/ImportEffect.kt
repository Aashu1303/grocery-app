package app.rasoi.feature.invoiceimport.upload

import app.rasoi.core.model.BillId

sealed interface ImportEffect {
    /** The server parsed the PDFs into a draft bill; open it for review. */
    data class BillImported(val billId: BillId) : ImportEffect
}
