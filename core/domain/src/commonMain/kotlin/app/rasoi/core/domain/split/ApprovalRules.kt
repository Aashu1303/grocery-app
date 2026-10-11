package app.rasoi.core.domain.split

import app.rasoi.core.domain.bills.BillRules
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Approval
import app.rasoi.core.model.ApprovalState
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.BillWarning
import app.rasoi.core.model.MemberShare
import app.rasoi.core.model.UserId
import app.rasoi.core.model.map
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * The bill lifecycle (design-plan 6.3). A draft is submitted to the members with a share; they approve or ask for
 * changes; silence for 48 hours counts as approval; an edit asks again only those whose share changed.
 */
object ApprovalRules {
    val AUTO_APPROVE_AFTER = 48.hours

    /** Members with a non-zero share, except the payer when the payer uploaded (and so submitted) the bill. */
    fun requiredApprovers(bill: Bill, shares: List<MemberShare>): Set<UserId> =
        shares.filter { !it.total.isZero && !(it.userId == bill.payerId && bill.createdBy == bill.payerId) }
            .map { it.userId }
            .toSet()

    fun submit(bill: Bill, submitter: UserId, now: Instant): AppResult<Bill> {
        val allowed = bill.status == BillStatus.DRAFT || bill.status == BillStatus.CHANGES_REQUESTED
        return when {
            submitter != bill.createdBy -> AppResult.Failure(AppError.Forbidden)
            !allowed -> AppResult.Failure(AppError.Conflict("bill_not_submittable"))
            BillRules.warnings(bill).any { it is BillWarning.TotalMismatch } -> invalid("total", "mismatch")
            else -> BillShares.of(bill).map { shares ->
                val approvals = requiredApprovers(bill, shares).map { user ->
                    bill.approvals.firstOrNull { it.userId == user && it.state == ApprovalState.APPROVED }
                        ?: Approval(user, ApprovalState.PENDING)
                }
                settle(bill.copy(approvals = approvals, submittedAt = now))
            }
        }
    }

    fun decide(bill: Bill, userId: UserId, state: ApprovalState, comment: String?, now: Instant): AppResult<Bill> {
        val own = bill.approvals.firstOrNull { it.userId == userId }
        return when {
            state == ApprovalState.PENDING -> invalid("state", "invalid")
            own == null -> AppResult.Failure(AppError.Forbidden)
            bill.status != BillStatus.PENDING -> AppResult.Failure(AppError.Conflict("bill_not_pending"))
            state == ApprovalState.CHANGES_REQUESTED && comment.isNullOrBlank() -> invalid("comment", "required")
            else -> {
                val decided = Approval(userId, state, comment?.trim()?.takeIf { it.isNotEmpty() }, now)
                AppResult.Success(
                    settle(
                        bill.copy(
                            approvals = bill.approvals.map {
                                if (it.userId ==
                                    userId
                                ) {
                                    decided
                                } else {
                                    it
                                }
                            },
                        ),
                    ),
                )
            }
        }
    }

    /** Gives the approvals still pending 48 hours after submission; returns [bill] itself when nothing is due. */
    fun autoApprove(bill: Bill, now: Instant): Bill {
        val submittedAt = bill.submittedAt
        val due = bill.status == BillStatus.PENDING && submittedAt != null && now >= submittedAt + AUTO_APPROVE_AFTER
        if (!due) return bill
        val approvals = bill.approvals.map {
            if (it.state ==
                ApprovalState.PENDING
            ) {
                Approval(it.userId, ApprovalState.APPROVED, null, now, automatic = true)
            } else {
                it
            }
        }
        return settle(bill.copy(approvals = approvals))
    }

    /**
     * An edit to a submitted bill: members whose share changed (or who are newly affected) are asked again, others keep
     * their approval. Returns [new] unchanged when nobody's share moved or the bill was still a draft.
     */
    fun afterEdit(old: Bill, new: Bill, now: Instant): Bill {
        val newShares = (BillShares.of(new) as? AppResult.Success)?.value
        val changed = if (newShares == null ||
            old.status == BillStatus.DRAFT
        ) {
            emptySet()
        } else {
            changedMembers(old, newShares)
        }
        if (newShares == null || changed.isEmpty()) return new
        val approvals = requiredApprovers(new, newShares).map { user ->
            val existing = new.approvals.firstOrNull { it.userId == user }
            if (existing != null && user !in changed) existing else Approval(user, ApprovalState.PENDING)
        }
        val resetClock = approvals.any { it.state == ApprovalState.PENDING }
        val edited = new.copy(approvals = approvals, submittedAt = if (resetClock) now else new.submittedAt)
        return if (new.status == BillStatus.CHANGES_REQUESTED) edited else settle(edited)
    }

    /** Members whose total moved, who are new to the bill, or who dropped off it. */
    private fun changedMembers(old: Bill, newShares: List<MemberShare>): Set<UserId> {
        val before = (BillShares.of(old) as? AppResult.Success)?.value.orEmpty().associate { it.userId to it.total }
        val moved = newShares.filter { before[it.userId] != it.total }.map { it.userId }
        val dropped = before.keys.filter { user -> newShares.none { it.userId == user } }
        return (moved + dropped).toSet()
    }

    /** Status follows the approvals: any change request wins, then all-approved, otherwise still pending. */
    private fun settle(bill: Bill): Bill {
        val states = bill.approvals.map { it.state }
        val status = when {
            ApprovalState.CHANGES_REQUESTED in states -> BillStatus.CHANGES_REQUESTED
            ApprovalState.PENDING in states -> BillStatus.PENDING
            else -> BillStatus.APPROVED
        }
        return bill.copy(status = status)
    }

    private fun invalid(field: String, code: String): AppResult<Bill> =
        AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation(field, code))))
}
