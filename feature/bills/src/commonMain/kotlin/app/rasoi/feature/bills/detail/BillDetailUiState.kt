package app.rasoi.feature.bills.detail

import app.rasoi.core.domain.bills.BillRules
import app.rasoi.core.domain.household.Permission
import app.rasoi.core.domain.household.RolePolicy
import app.rasoi.core.domain.split.BillShares
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Approval
import app.rasoi.core.model.ApprovalState
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.BillWarning
import app.rasoi.core.model.ChargeId
import app.rasoi.core.model.Member
import app.rasoi.core.model.MemberShare
import app.rasoi.core.model.Role
import app.rasoi.core.model.SplitMode
import app.rasoi.core.model.UserId
import app.rasoi.feature.bills.BillsError
import app.rasoi.feature.bills.FieldError

sealed interface BillDetailUiState {
    data object Loading : BillDetailUiState

    data class Error(val error: BillsError) : BillDetailUiState

    /**
     * A bill as seen by [myUserId]. [isBusy] covers any save in flight; [error] is shown inside the open [sheet], or
     * above the bill when none is open.
     */
    data class Content(
        val bill: Bill,
        val members: List<Member>,
        val myUserId: UserId,
        val myRole: Role,
        val sheet: BillSheet? = null,
        val isBusy: Boolean = false,
        val error: BillsError? = null,
        /** The user pulled to refresh; the content stays while it reloads. */
        val isRefreshing: Boolean = false,
    ) : BillDetailUiState {
        val names: Map<UserId, String> get() = members.associate { it.userId to it.displayName }

        /** Cooks never owe a share, so they are not offered when splitting. */
        val participants: List<Member>
            get() = members.filter { RolePolicy.allows(it.role, Permission.APPROVE_BILLS) }

        val shares: List<MemberShare> get() = (BillShares.of(bill) as? AppResult.Success)?.value.orEmpty()

        val mismatch: BillWarning.TotalMismatch?
            get() = BillRules.warnings(bill).filterIsInstance<BillWarning.TotalMismatch>().firstOrNull()

        val isUploader: Boolean get() = bill.createdBy == myUserId

        private val mayEditBills: Boolean get() = RolePolicy.allows(myRole, Permission.EDIT_BILLS)

        /** Drafts belong to their uploader; a submitted bill is edited by its uploader or an owner until it locks. */
        val canEdit: Boolean
            get() = mayEditBills &&
                bill.status != BillStatus.LOCKED &&
                (bill.status == BillStatus.DRAFT || isUploader || myRole == Role.OWNER)

        val canEditItems: Boolean get() = mayEditBills && isUploader && bill.status == BillStatus.DRAFT

        val canSubmit: Boolean
            get() = mayEditBills &&
                isUploader &&
                (bill.status == BillStatus.DRAFT || bill.status == BillStatus.CHANGES_REQUESTED)

        val myApproval: Approval? get() = bill.approvals.firstOrNull { it.userId == myUserId }

        private val canDecide: Boolean get() = bill.status == BillStatus.PENDING && myApproval != null

        val canApprove: Boolean get() = canDecide && myApproval?.state != ApprovalState.APPROVED

        val canRequestChanges: Boolean get() = canDecide && myApproval?.state != ApprovalState.CHANGES_REQUESTED
    }
}

/** A sheet over the bill. Its inputs live here; saving and failures are shown from [BillDetailUiState.Content]. */
sealed interface BillSheet {
    /** [units] holds the chosen people in order; the counts only matter for [SplitMode.QUANTITY]. */
    data class Split(
        val lineId: BillLineId,
        val mode: SplitMode,
        val units: Map<UserId, Int>,
        val error: SplitError? = null,
    ) : BillSheet

    data object Payer : BillSheet

    data class Charge(val chargeId: ChargeId) : BillSheet

    data class RequestChanges(val comment: String = "", val commentError: FieldError? = null) : BillSheet
}

/** What is wrong with a split before it is sent. */
enum class SplitError { NO_MEMBERS, ONE_MEMBER, UNITS }
