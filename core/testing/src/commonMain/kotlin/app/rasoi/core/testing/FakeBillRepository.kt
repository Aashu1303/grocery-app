package app.rasoi.core.testing

import app.rasoi.core.domain.bills.BillEdit
import app.rasoi.core.domain.bills.BillRepository
import app.rasoi.core.domain.bills.BillRules
import app.rasoi.core.domain.bills.InvoiceFile
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.ChargeId
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.flatMap

/** In-memory bills; imports return [nextImport]. Set [failure] to make every call fail with that error. */
class FakeBillRepository(var nextImport: Bill = Builders.bill()) : BillRepository {
    var failure: AppError? = null
    val bills = mutableMapOf<BillId, Bill>()
    val imports = mutableListOf<List<InvoiceFile>>()
    val updates = mutableListOf<Pair<BillId, BillEdit>>()
    private var nextId = 0

    override suspend fun importInvoices(householdId: HouseholdId, files: List<InvoiceFile>): AppResult<Bill> = result {
        imports += files
        AppResult.Success(nextImport.also { bills[it.id] = it })
    }

    override suspend fun bill(id: BillId): AppResult<Bill> = result {
        bills[id]?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.NotFound("bill"))
    }

    override suspend fun update(id: BillId, edit: BillEdit): AppResult<Bill> = result {
        updates += id to edit
        bill(id).flatMap { bill ->
            BillRules.apply(bill, edit, { BillLineId("new${nextId++}") }, { ChargeId("charge${nextId++}") })
        }.also { if (it is AppResult.Success) bills[id] = it.value }
    }

    private inline fun <T> result(block: () -> AppResult<T>): AppResult<T> =
        failure?.let { AppResult.Failure(it) } ?: block()
}
