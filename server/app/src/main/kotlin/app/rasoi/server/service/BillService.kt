package app.rasoi.server.service

import app.rasoi.core.domain.bills.BillEdit
import app.rasoi.core.domain.bills.BillRules
import app.rasoi.core.domain.household.Permission
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.UserId
import app.rasoi.core.model.flatMap
import app.rasoi.core.model.map
import app.rasoi.core.model.mapError
import app.rasoi.server.persistence.BillStore
import app.rasoi.server.persistence.PantryChanges
import app.rasoi.server.persistence.PantryStore
import app.rasoi.server.persistence.toBillLineId
import app.rasoi.server.persistence.toChargeId
import kotlin.time.Clock
import kotlin.uuid.Uuid

/**
 * Reading and editing bills. A draft is visible only to its uploader; anyone else, and every non-member, gets
 * `NotFound("bill")` so a bill's existence is never revealed.
 */
class BillService(
    private val households: HouseholdService,
    private val bills: BillStore,
    private val pantry: PantryStore,
    private val planner: PantryPlanner,
    private val clock: Clock,
) {
    suspend fun get(userId: UserId, billId: BillId): AppResult<Bill> = load(userId, billId, Permission.VIEW_BILLS)

    suspend fun update(userId: UserId, billId: BillId, edit: BillEdit): AppResult<Bill> =
        load(userId, billId, Permission.EDIT_BILLS)
            .flatMap { bill -> BillRules.ensureEditable(bill).map { bill } }
            .flatMap { bill -> BillRules.validate(edit).map { bill } }
            .flatMap { bill ->
                BillRules.apply(bill, edit, { Uuid.random().toBillLineId() }, { Uuid.random().toChargeId() })
            }
            .map { bills.updateContents(it, pantryChanges(it), clock.now()) }

    /** Food lines without a pantry item get one; lines no longer food lose their untouched item. */
    private suspend fun pantryChanges(bill: Bill): PantryChanges {
        val existing = pantry.lineIdsWithItems(bill.lines.map { it.id })
        val missing = bill.lines.filter { it.isFood && it.id !in existing }
        return PantryChanges(
            add = planner.itemsFor(bill, missing),
            removeForLines = bill.lines.filterNot { it.isFood }.map { it.id },
        )
    }

    private suspend fun load(userId: UserId, billId: BillId, permission: Permission): AppResult<Bill> {
        val bill = bills.find(billId)
        if (bill == null || (bill.status == BillStatus.DRAFT && bill.createdBy != userId)) {
            return AppResult.Failure(AppError.NotFound("bill"))
        }
        return households.authorize(userId, bill.householdId, permission)
            .mapError { if (it is AppError.NotFound) AppError.NotFound("bill") else it }
            .map { bill }
    }
}
