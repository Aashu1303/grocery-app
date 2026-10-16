package app.rasoi.app

import app.rasoi.core.domain.bills.InvoiceFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update

/**
 * Invoice PDFs another app handed to Rasoi through "Share" or "Open with". They wait here, through sign-in if
 * needed, until the import screen takes them.
 */
class IncomingInvoices {
    private val pending = MutableStateFlow<List<InvoiceFile>>(emptyList())

    val files: StateFlow<List<InvoiceFile>> = pending.asStateFlow()

    fun offer(files: List<InvoiceFile>) {
        if (files.isNotEmpty()) pending.update { it + files }
    }

    fun take(): List<InvoiceFile> = pending.getAndUpdate { emptyList() }
}
