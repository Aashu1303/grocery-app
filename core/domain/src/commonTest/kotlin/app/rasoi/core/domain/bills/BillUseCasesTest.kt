package app.rasoi.core.domain.bills

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillId
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Money
import app.rasoi.core.model.Role
import app.rasoi.core.testing.Builders
import app.rasoi.core.testing.FakeBillRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BillUseCasesTest {
    private val repository = FakeBillRepository()
    private val householdId = HouseholdId("h1")
    private val pdf = InvoiceFile("order.pdf", byteArrayOf(1, 2, 3))

    private fun invalid(field: String, code: String) =
        AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation(field, code))))

    @Test
    fun importSendsTheFilesAndReturnsTheDraft() = runTest {
        val result = ImportInvoicesUseCase(repository)(householdId, Role.MEMBER, listOf(pdf))
        assertIs<AppResult.Success<*>>(result)
        assertEquals(listOf(listOf("order.pdf")), repository.imports.map { files -> files.map { it.name } })
    }

    @Test
    fun importChecksRoleCountAndSizeBeforeUploading() = runTest {
        val import = ImportInvoicesUseCase(repository)
        assertEquals(AppResult.Failure(AppError.Forbidden), import(householdId, Role.COOK, listOf(pdf)))
        assertEquals(invalid("documents", "count"), import(householdId, Role.OWNER, emptyList()))
        val tooMany = List(ImportInvoicesUseCase.MAX_FILES + 1) { pdf }
        assertEquals(invalid("documents", "count"), import(householdId, Role.OWNER, tooMany))
        assertEquals(
            invalid("documents[0]", "empty"),
            import(householdId, Role.OWNER, listOf(InvoiceFile("a.pdf", ByteArray(0)))),
        )
        val big = InvoiceFile("big.pdf", ByteArray(ImportInvoicesUseCase.MAX_TOTAL_BYTES + 1))
        assertEquals(invalid("documents", "too_large"), import(householdId, Role.OWNER, listOf(big)))
        assertTrue(repository.imports.isEmpty())
    }

    @Test
    fun getReturnsTheRepositoryBill() = runTest {
        val bill = Builders.bill()
        repository.bills[bill.id] = bill
        assertEquals(AppResult.Success(bill), GetBillUseCase(repository)(bill.id))
        assertEquals(AppResult.Failure(AppError.NotFound("bill")), GetBillUseCase(repository)(BillId("missing")))
    }

    @Test
    fun updateValidatesLocallyBeforeSaving() = runTest {
        val bill = Builders.bill()
        repository.bills[bill.id] = bill
        val update = UpdateBillUseCase(repository)
        assertEquals(invalid("total", "negative"), update(bill.id, BillEdit(total = Money(-1))))
        assertTrue(repository.updates.isEmpty())
        val saved = assertIs<AppResult.Success<Bill>>(update(bill.id, BillEdit(total = Money(9_999))))
        assertEquals(Money(9_999), saved.value.total)
        assertEquals(1, repository.updates.size)
    }

    @Test
    fun failuresComeBackAsTypedErrors() = runTest {
        repository.failure = AppError.Offline
        assertEquals(
            AppResult.Failure(AppError.Offline),
            ImportInvoicesUseCase(repository)(householdId, Role.OWNER, listOf(pdf)),
        )
    }
}
