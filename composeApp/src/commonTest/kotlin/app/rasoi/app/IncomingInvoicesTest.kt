package app.rasoi.app

import app.rasoi.core.domain.bills.InvoiceFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IncomingInvoicesTest {
    private fun pdf(name: String) = InvoiceFile(name, byteArrayOf(1))

    @Test
    fun sharedFilesWaitUntilTakenAndAreHandedOverOnce() {
        val incoming = IncomingInvoices()
        incoming.offer(listOf(pdf("a.pdf"), pdf("b.pdf")))
        assertEquals(listOf("a.pdf", "b.pdf"), incoming.files.value.map { it.name })
        assertEquals(listOf("a.pdf", "b.pdf"), incoming.take().map { it.name })
        assertTrue(incoming.files.value.isEmpty())
        assertTrue(incoming.take().isEmpty())
    }

    @Test
    fun aSecondShareBeforeImportAddsToTheFirstAndAnEmptyShareIsIgnored() {
        val incoming = IncomingInvoices()
        incoming.offer(listOf(pdf("a.pdf")))
        incoming.offer(emptyList())
        incoming.offer(listOf(pdf("b.pdf")))
        assertEquals(listOf("a.pdf", "b.pdf"), incoming.take().map { it.name })
    }
}
