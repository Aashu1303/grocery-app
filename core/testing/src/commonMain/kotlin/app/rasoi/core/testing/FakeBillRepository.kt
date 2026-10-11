package app.rasoi.core.testing

import app.rasoi.core.domain.bills.BillEdit
import app.rasoi.core.domain.bills.BillRepository
import app.rasoi.core.domain.bills.BillRules
import app.rasoi.core.domain.bills.InvoiceFile
import app.rasoi.core.domain.bills.ManualBillDraft
import app.rasoi.core.domain.bills.ManualBills
import app.rasoi.core.domain.split.ApprovalRules
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.ApprovalState
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.BillSummary
import app.rasoi.core.model.ChargeId
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.UserId
import app.rasoi.core.model.flatMap
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * In-memory bills that apply the same domain rules as the server; imports return [nextImport]. Set [failure] to make
 * every call fail with that error. [me] is who the fake acts as when it submits or decides.
 */
class FakeBillRepository(
    var nextImport: Bill = Builders.bill(),
    var me: UserId = UserId("u1"),
    private val clock: Clock = FixedClock(),
) : BillRepository {
    var failure: AppError? = null
    val bills = mutableMapOf<BillId, Bill>()
    val imports = mutableListOf<List<InvoiceFile>>()
    val updates = mutableListOf<Pair<BillId, BillEdit>>()
    val submitted = mutableListOf<BillId>()
    val decisions = mutableListOf<Triple<BillId, ApprovalState, String?>>()
    private var nextId = 0

    override suspend fun importInvoices(householdId: HouseholdId, files: List<InvoiceFile>): AppResult<Bill> = result {
        imports += files
        AppResult.Success(nextImport.also { bills[it.id] = it })
    }

    override suspend fun bills(householdId: HouseholdId): AppResult<List<BillSummary>> = result {
        AppResult.Success(
            bills.values.filter { it.householdId == householdId }.sortedByDescending { it.invoiceDate }.map {
                BillSummary(
                    it.id, it.platform, it.title, it.invoiceDate, it.payerId, it.createdBy, it.status, it.total,
                    it.lines.size,
                )
            },
        )
    }

    override suspend fun createManual(householdId: HouseholdId, draft: ManualBillDraft): AppResult<Bill> = result {
        ManualBills.create(
            id = BillId("manual${nextId++}"),
            householdId = householdId,
            creator = me,
            draft = draft,
            today = clock.now().toLocalDateTime(TimeZone.UTC).date,
            newLineId = { BillLineId("new${nextId++}") },
            newChargeId = { ChargeId("charge${nextId++}") },
        ).also { if (it is AppResult.Success) bills[it.value.id] = it.value }
    }

    override suspend fun bill(id: BillId): AppResult<Bill> = result {
        bills[id]?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.NotFound("bill"))
    }

    override suspend fun update(id: BillId, edit: BillEdit): AppResult<Bill> = result {
        updates += id to edit
        bill(id).flatMap { bill ->
            BillRules.apply(bill, edit, { BillLineId("new${nextId++}") }, { ChargeId("charge${nextId++}") })
        }.flatMap { edited ->
            val old = bills.getValue(id)
            if (old.status == BillStatus.DRAFT) {
                AppResult.Success(edited)
            } else {
                AppResult.Success(ApprovalRules.afterEdit(old, edited, clock.now()))
            }
        }.also { if (it is AppResult.Success) bills[id] = it.value }
    }

    override suspend fun submit(id: BillId): AppResult<Bill> = result {
        submitted += id
        bill(id).flatMap { ApprovalRules.submit(it, me, clock.now()) }
            .also { if (it is AppResult.Success) bills[id] = it.value }
    }

    override suspend fun decide(id: BillId, state: ApprovalState, comment: String?): AppResult<Bill> = result {
        decisions += Triple(id, state, comment)
        bill(id).flatMap { ApprovalRules.decide(it, me, state, comment, clock.now()) }
            .also { if (it is AppResult.Success) bills[id] = it.value }
    }

    private inline fun <T> result(block: () -> AppResult<T>): AppResult<T> =
        failure?.let { AppResult.Failure(it) } ?: block()
}
