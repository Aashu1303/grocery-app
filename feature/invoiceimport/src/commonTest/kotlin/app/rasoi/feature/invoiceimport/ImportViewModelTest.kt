package app.rasoi.feature.invoiceimport

import app.cash.turbine.test
import app.rasoi.core.domain.bills.ImportInvoicesUseCase
import app.rasoi.core.domain.bills.InvoiceFile
import app.rasoi.core.model.AppError
import app.rasoi.core.model.BillId
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Role
import app.rasoi.core.testing.Builders
import app.rasoi.core.testing.FakeBillRepository
import app.rasoi.feature.invoiceimport.upload.ImportEffect
import app.rasoi.feature.invoiceimport.upload.ImportUiEvent
import app.rasoi.feature.invoiceimport.upload.ImportViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImportViewModelTest : ViewModelTest() {
    private val repository = FakeBillRepository(Builders.bill(id = "bill-7"))

    private fun viewModel(role: Role = Role.MEMBER) =
        ImportViewModel(HouseholdId("h1"), role, ImportInvoicesUseCase(repository))

    private fun pdf(name: String) = InvoiceFile(name, byteArrayOf(1, 2, 3))

    @Test
    fun startsEmptyAndCannotUpload() {
        val state = viewModel().state.value
        assertTrue(state.files.isEmpty())
        assertFalse(state.canUpload)
        assertTrue(state.canAddMore)
    }

    @Test
    fun pickedFilesAccumulateUpToTheLimitAndCanBeRemoved() {
        val viewModel = viewModel()
        viewModel.onEvent(ImportUiEvent.FilesPicked(listOf(pdf("a.pdf"), pdf("b.pdf"))))
        viewModel.onEvent(ImportUiEvent.FilesPicked(List(5) { pdf("more$it.pdf") }))
        assertEquals(ImportInvoicesUseCase.MAX_FILES, viewModel.state.value.files.size)
        assertFalse(viewModel.state.value.canAddMore)

        viewModel.onEvent(ImportUiEvent.RemoveFile(0))
        assertEquals("b.pdf", viewModel.state.value.files.first().name)
        assertTrue(viewModel.state.value.canAddMore)
    }

    @Test
    fun uploadSendsTheFilesAndOpensTheDraft() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(ImportUiEvent.FilesPicked(listOf(pdf("a.pdf"))))
        viewModel.effects.test {
            viewModel.onEvent(ImportUiEvent.Upload)
            assertEquals(ImportEffect.BillImported(BillId("bill-7")), awaitItem())
        }
        assertEquals(listOf(listOf("a.pdf")), repository.imports.map { files -> files.map { it.name } })
        assertFalse(viewModel.state.value.isUploading)
    }

    @Test
    fun uploadWithoutFilesDoesNothing() {
        val viewModel = viewModel()
        viewModel.onEvent(ImportUiEvent.Upload)
        assertTrue(repository.imports.isEmpty())
    }

    @Test
    fun failuresShowAMessageAndKeepTheFilesForRetry() {
        val viewModel = viewModel()
        viewModel.onEvent(ImportUiEvent.FilesPicked(listOf(pdf("a.pdf"))))

        repository.failure = AppError.Conflict("duplicate_invoice")
        viewModel.onEvent(ImportUiEvent.Upload)
        assertEquals(BillError.DUPLICATE, viewModel.state.value.error)
        assertEquals(1, viewModel.state.value.files.size)
        assertFalse(viewModel.state.value.isUploading)

        repository.failure = null
        viewModel.onEvent(ImportUiEvent.Upload)
        assertEquals(null, viewModel.state.value.error)
    }

    @Test
    fun aCookCannotImport() {
        val viewModel = viewModel(Role.COOK)
        viewModel.onEvent(ImportUiEvent.FilesPicked(listOf(pdf("a.pdf"))))
        viewModel.onEvent(ImportUiEvent.Upload)
        assertEquals(BillError.FORBIDDEN, viewModel.state.value.error)
        assertTrue(repository.imports.isEmpty())
    }

    @Test
    fun changingTheFilesClearsTheError() {
        val viewModel = viewModel()
        viewModel.onEvent(ImportUiEvent.FilesPicked(listOf(pdf("a.pdf"))))
        repository.failure = AppError.Offline
        viewModel.onEvent(ImportUiEvent.Upload)
        assertEquals(BillError.OFFLINE, viewModel.state.value.error)
        viewModel.onEvent(ImportUiEvent.RemoveFile(0))
        assertEquals(null, viewModel.state.value.error)
    }
}
