package app.rasoi.server.service

import app.rasoi.core.domain.bills.BillEdit
import app.rasoi.core.domain.bills.BillRules
import app.rasoi.core.domain.bills.ManualBillDraft
import app.rasoi.core.domain.bills.ManualBills
import app.rasoi.core.domain.household.Permission
import app.rasoi.core.domain.split.ApprovalRules
import app.rasoi.core.domain.split.BillShares
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.ApprovalState
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Role
import app.rasoi.core.model.UserId
import app.rasoi.core.model.flatMap
import app.rasoi.core.model.map
import app.rasoi.core.model.mapError
import app.rasoi.server.notifications.PushMessage
import app.rasoi.server.persistence.BillStore
import app.rasoi.server.persistence.PantryChanges
import app.rasoi.server.persistence.PantryStore
import app.rasoi.server.persistence.toBillId
import app.rasoi.server.persistence.toBillLineId
import app.rasoi.server.persistence.toChargeId
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.uuid.Uuid

/**
 * Reading, editing, submitting and approving bills (design-plan 6.3). A draft is visible only to its uploader; anyone
 * else, and every non-member, gets `NotFound("bill")` so a bill's existence is never revealed. Once submitted, only the
 * uploader or an owner may still edit it.
 */
class BillService(
    private val households: HouseholdService,
    private val bills: BillStore,
    private val pantry: PantryStore,
    private val planner: PantryPlanner,
    private val periods: PeriodService,
    private val notifications: NotificationService,
    private val clock: Clock,
) {
    private class Loaded(val bill: Bill, val role: Role)

    suspend fun get(userId: UserId, billId: BillId): AppResult<Bill> =
        load(userId, billId, Permission.VIEW_BILLS).map { it.bill }

    /** The household's bills, newest first, without other members' drafts. */
    suspend fun list(userId: UserId, householdId: HouseholdId): AppResult<List<Bill>> =
        households.authorize(userId, householdId, Permission.VIEW_BILLS).map {
            bills.list(householdId).filter { it.status != BillStatus.DRAFT || it.createdBy == userId }
        }

    suspend fun createManual(userId: UserId, householdId: HouseholdId, draft: ManualBillDraft): AppResult<Bill> =
        households.authorize(userId, householdId, Permission.EDIT_BILLS)
            .flatMap { ensureParticipants(householdId, listOfNotNull(draft.payerId), "payerId") }
            .flatMap {
                ManualBills.create(
                    id = Uuid.random().toBillId(),
                    householdId = householdId,
                    creator = userId,
                    draft = draft,
                    today = clock.now().toLocalDateTime(households.timeZone(householdId)).date,
                    newLineId = { Uuid.random().toBillLineId() },
                    newChargeId = { Uuid.random().toChargeId() },
                )
            }
            .map { it.copy(periodId = periods.current(householdId).id) }
            .flatMap { bills.create(it, emptyList(), planner.itemsFor(it), clock.now()) }

    suspend fun update(userId: UserId, billId: BillId, edit: BillEdit): AppResult<Bill> =
        load(userId, billId, Permission.EDIT_BILLS)
            .flatMap { loaded -> ensureMayEdit(loaded, userId).map { loaded.bill } }
            .flatMap { bill -> BillRules.validate(edit).map { bill } }
            .flatMap { bill -> ensureParticipants(bill.householdId, mentionedUsers(edit), "members").map { bill } }
            .flatMap { bill ->
                BillRules.apply(bill, edit, { Uuid.random().toBillLineId() }, { Uuid.random().toChargeId() })
                    .flatMap { edited -> reapprove(bill, edited) }
            }
            .flatMap { bills.save(it, pantryChanges(it), clock.now()) }

    /** Sends a draft (or a bill with change requests) to the members who owe a share. */
    suspend fun submit(userId: UserId, billId: BillId): AppResult<Bill> = load(userId, billId, Permission.EDIT_BILLS)
        .flatMap { ApprovalRules.submit(it.bill, userId, clock.now()) }
        .flatMap { bills.save(it, PantryChanges(), clock.now()) }
        .also { result ->
            if (result is AppResult.Success) {
                val waiting = result.value.approvals.filter { it.state == ApprovalState.PENDING }.map { it.userId }
                notifications.notify(
                    waiting,
                    PushMessage(
                        "Bill waiting for approval",
                        billName(result.value),
                        mapOf(
                            "billId" to result.value.id.value,
                        ),
                    ),
                )
            }
        }

    suspend fun decide(userId: UserId, billId: BillId, state: ApprovalState, comment: String?): AppResult<Bill> =
        load(userId, billId, Permission.APPROVE_BILLS)
            .flatMap { ApprovalRules.decide(it.bill, userId, state, comment, clock.now()) }
            .flatMap { bills.save(it, PantryChanges(), clock.now()) }
            .also { result ->
                if (result is AppResult.Success && state == ApprovalState.CHANGES_REQUESTED) {
                    val bill = result.value
                    notifications.notify(
                        listOf(bill.createdBy),
                        PushMessage(
                            "Changes requested",
                            billName(bill),
                            mapOf(
                                "billId" to bill.id.value,
                            ),
                        ),
                    )
                }
            }

    /** Gives the approvals that have been pending for 48 hours; returns how many bills moved. */
    suspend fun autoApproveDue(): Int {
        val now = clock.now()
        val due = bills.pendingSince(now - ApprovalRules.AUTO_APPROVE_AFTER)
        return due.map { ApprovalRules.autoApprove(it, now) }
            .filter { it.status != BillStatus.PENDING }
            .count { bills.save(it, PantryChanges(), now) is AppResult.Success }
    }

    private fun billName(bill: Bill): String = bill.title ?: bill.invoiceNumber

    private fun ensureMayEdit(loaded: Loaded, userId: UserId): AppResult<Unit> =
        if (loaded.bill.status == BillStatus.DRAFT || loaded.bill.createdBy == userId || loaded.role == Role.OWNER) {
            BillRules.ensureEditable(loaded.bill)
        } else {
            AppResult.Failure(AppError.Forbidden)
        }

    /** A submitted bill must stay fully split, and members whose share moved are asked again. */
    private fun reapprove(old: Bill, edited: Bill): AppResult<Bill> = if (old.status == BillStatus.DRAFT) {
        AppResult.Success(edited)
    } else {
        BillShares.of(edited).map { ApprovalRules.afterEdit(old, edited, clock.now()) }
    }

    private fun mentionedUsers(edit: BillEdit): List<UserId> =
        listOfNotNull(edit.payerId) + edit.splits.orEmpty().flatMap { it.split.entries }.map { it.userId }

    private suspend fun ensureParticipants(
        householdId: HouseholdId,
        users: List<UserId>,
        field: String,
    ): AppResult<Unit> {
        val members = households.billParticipants(householdId)
        return if (members.containsAll(users)) {
            AppResult.Success(Unit)
        } else {
            AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation(field, "not_a_member"))))
        }
    }

    /** Food lines without a pantry item get one; lines no longer food lose their untouched item. */
    private suspend fun pantryChanges(bill: Bill): PantryChanges {
        val existing = pantry.lineIdsWithItems(bill.lines.map { it.id })
        val missing = bill.lines.filter { it.isFood && it.id !in existing }
        return PantryChanges(
            add = planner.itemsFor(bill, missing),
            removeForLines = bill.lines.filterNot { it.isFood }.map { it.id },
        )
    }

    private suspend fun load(userId: UserId, billId: BillId, permission: Permission): AppResult<Loaded> {
        val bill = bills.find(billId)
        if (bill == null || (bill.status == BillStatus.DRAFT && bill.createdBy != userId)) {
            return AppResult.Failure(AppError.NotFound("bill"))
        }
        return households.authorize(userId, bill.householdId, permission)
            .mapError { if (it is AppError.NotFound) AppError.NotFound("bill") else it }
            .map { Loaded(bill, it) }
    }
}
