package app.rasoi.feature.bills.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rasoi.core.domain.bills.BillEdit
import app.rasoi.core.domain.bills.ChargeEdit
import app.rasoi.core.domain.bills.DecideBillUseCase
import app.rasoi.core.domain.bills.GetBillUseCase
import app.rasoi.core.domain.bills.LineSplitEdit
import app.rasoi.core.domain.bills.SubmitBillUseCase
import app.rasoi.core.domain.bills.UpdateBillUseCase
import app.rasoi.core.domain.household.HouseholdRepository
import app.rasoi.core.domain.split.LineAllocator
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.ApprovalState
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.ChargeDistribution
import app.rasoi.core.model.LineSplit
import app.rasoi.core.model.Role
import app.rasoi.core.model.SplitEntry
import app.rasoi.core.model.SplitMode
import app.rasoi.core.model.UserId
import app.rasoi.feature.bills.BillsError
import app.rasoi.feature.bills.FieldError
import app.rasoi.feature.bills.billsError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One bill: who paid, how each item is split, what everyone owes, and where it stands in the approval flow. Every
 * change is saved to the server straight away; the server is authoritative (design-plan principle 6).
 */
class BillDetailViewModel(
    private val billId: BillId,
    private val me: UserId,
    private val myRole: Role,
    private val households: HouseholdRepository,
    private val getBill: GetBillUseCase,
    private val updateBill: UpdateBillUseCase,
    private val submitBill: SubmitBillUseCase,
    private val decideBill: DecideBillUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow<BillDetailUiState>(BillDetailUiState.Loading)
    val state: StateFlow<BillDetailUiState> = mutableState.asStateFlow()

    private val effectChannel = Channel<BillDetailEffect>(Channel.BUFFERED)
    val effects: Flow<BillDetailEffect> = effectChannel.receiveAsFlow()

    init {
        load()
    }

    fun onEvent(event: BillDetailUiEvent) {
        when (event) {
            BillDetailUiEvent.Retry -> load()
            BillDetailUiEvent.Refresh -> refresh()
            BillDetailUiEvent.DismissSheet -> updateContent { it.copy(sheet = null, error = null) }
            is BillDetailUiEvent.OpenSplit -> openSplit(event.lineId)
            BillDetailUiEvent.OpenPayer -> openSheet(BillSheet.Payer)
            is BillDetailUiEvent.OpenCharge -> openSheet(BillSheet.Charge(event.chargeId))
            BillDetailUiEvent.EditItems -> viewModelScope.launch {
                effectChannel.send(BillDetailEffect.OpenItems(billId))
            }
            BillDetailUiEvent.Submit -> withBill { save { submitBill(it, me) } }
            BillDetailUiEvent.Approve -> withBill { save { decideBill(it, me, ApprovalState.APPROVED, null) } }
            BillDetailUiEvent.OpenRequestChanges -> openSheet(BillSheet.RequestChanges())
            is BillDetailUiEvent.Split -> onSplit(event)
            is BillDetailUiEvent.Change -> onChange(event)
        }
    }

    private fun onSplit(event: BillDetailUiEvent.Split) = when (event) {
        is BillDetailUiEvent.Split.ModeSelected -> updateSplit { it.withMode(event.mode) }
        is BillDetailUiEvent.Split.MemberToggled -> updateSplit { it.toggled(event.userId) }
        is BillDetailUiEvent.Split.UnitsStepped -> stepUnits(event.userId, event.delta)
        BillDetailUiEvent.Split.Save -> saveSplit()
    }

    private fun onChange(event: BillDetailUiEvent.Change) = when (event) {
        is BillDetailUiEvent.Change.PayerSelected -> withBill {
            save { updateBill(billId, BillEdit(payerId = event.userId)) }
        }
        is BillDetailUiEvent.Change.DistributionSelected -> setDistribution(event.distribution)
        is BillDetailUiEvent.Change.CommentChanged -> updateSheet<BillSheet.RequestChanges> {
            it.copy(comment = event.value, commentError = null)
        }
        BillDetailUiEvent.Change.SendChangeRequest -> sendChangeRequest()
    }

    /** Replaces the bill with the server's copy, keeping the screen as it is when that fails. */
    private fun refresh() {
        if (mutableState.value !is BillDetailUiState.Content) return
        viewModelScope.launch {
            (getBill(billId) as? AppResult.Success)?.let { fresh ->
                updateContent { if (it.sheet == null && !it.isBusy) it.copy(bill = fresh.value) else it }
            }
        }
    }

    private fun load() {
        mutableState.value = BillDetailUiState.Loading
        viewModelScope.launch {
            mutableState.value = when (val bill = getBill(billId)) {
                is AppResult.Failure -> BillDetailUiState.Error(bill.error.billsError())
                is AppResult.Success -> when (val details = households.details(bill.value.householdId)) {
                    is AppResult.Failure -> BillDetailUiState.Error(details.error.billsError())
                    is AppResult.Success ->
                        BillDetailUiState.Content(bill.value, details.value.members, me, myRole)
                }
            }
        }
    }

    private fun openSplit(lineId: BillLineId) = updateContent { content ->
        if (!content.canEdit || content.bill.lines.none { it.id == lineId }) return@updateContent content
        val existing = content.bill.allocations.filter { it.lineId == lineId }
        val sheet = if (existing.isEmpty()) {
            BillSheet.Split(lineId, SplitMode.EQUAL, content.participants.associate { it.userId to 1 })
        } else {
            BillSheet.Split(lineId, existing.first().mode, existing.associate { it.userId to it.units })
        }
        content.copy(sheet = sheet, error = null)
    }

    private fun openSheet(sheet: BillSheet) = updateContent {
        if (it.canEdit ||
            sheet is BillSheet.RequestChanges
        ) {
            it.copy(sheet = sheet, error = null)
        } else {
            it
        }
    }

    private fun stepUnits(userId: UserId, delta: Int) {
        val content = mutableState.value as? BillDetailUiState.Content ?: return
        val sheet = content.sheet as? BillSheet.Split ?: return
        val maxUnits = content.bill.lines.firstOrNull { it.id == sheet.lineId }?.units ?: return
        updateSplit { split ->
            val current = split.units[userId] ?: return@updateSplit split
            split.copy(units = split.units + (userId to (current + delta).coerceIn(1, maxUnits)), error = null)
        }
    }

    private fun saveSplit() {
        val content = mutableState.value as? BillDetailUiState.Content
        val sheet = content?.sheet as? BillSheet.Split
        val line = content?.bill?.lines?.firstOrNull { it.id == sheet?.lineId }
        if (sheet == null || line == null) return
        val entries = sheet.units.map { (user, units) ->
            SplitEntry(user, if (sheet.mode == SplitMode.QUANTITY) units else 1)
        }
        val split = LineSplit(sheet.mode, entries)
        when (val local = LineAllocator.allocate(line, split)) {
            is AppResult.Failure -> updateSplit { it.copy(error = local.error.splitError()) }
            is AppResult.Success -> save {
                updateBill(billId, BillEdit(splits = listOf(LineSplitEdit(line.id, split))))
            }
        }
    }

    private fun setDistribution(distribution: ChargeDistribution) = withBill { bill ->
        val chosen = (mutableState.value as? BillDetailUiState.Content)?.sheet as? BillSheet.Charge ?: return@withBill
        val charges = bill.charges.map { charge ->
            ChargeEdit(
                charge.id,
                charge.kind,
                charge.label,
                charge.amount,
                distribution.takeIf {
                    charge.id ==
                        chosen.chargeId
                },
            )
        }
        save { updateBill(billId, BillEdit(charges = charges)) }
    }

    private fun sendChangeRequest() = withBill { bill ->
        val sheet = (mutableState.value as? BillDetailUiState.Content)?.sheet as? BillSheet.RequestChanges
            ?: return@withBill
        if (sheet.comment.isBlank()) {
            updateSheet<BillSheet.RequestChanges> { it.copy(commentError = FieldError.REQUIRED) }
            return@withBill
        }
        save { decideBill(bill, me, ApprovalState.CHANGES_REQUESTED, sheet.comment) }
    }

    /** Runs one save; success replaces the bill and closes the sheet, failure keeps the sheet open with the error. */
    private fun save(block: suspend () -> AppResult<Bill>) {
        val content = mutableState.value as? BillDetailUiState.Content ?: return
        if (content.isBusy) return
        updateContent { it.copy(isBusy = true, error = null) }
        viewModelScope.launch {
            when (val result = block()) {
                is AppResult.Success -> updateContent { it.copy(bill = result.value, sheet = null, isBusy = false) }
                is AppResult.Failure -> fail(result.error)
            }
        }
    }

    private fun fail(error: AppError) {
        val shown = error.billsError()
        updateContent { it.copy(isBusy = false, error = shown) }
        if (shown == BillsError.CHANGED) {
            viewModelScope.launch {
                (getBill(billId) as? AppResult.Success)?.let { fresh ->
                    updateContent { it.copy(bill = fresh.value, sheet = null) }
                }
            }
        }
    }

    private fun withBill(block: (Bill) -> Unit) {
        (mutableState.value as? BillDetailUiState.Content)?.let { block(it.bill) }
    }

    private fun updateContent(transform: (BillDetailUiState.Content) -> BillDetailUiState.Content) {
        mutableState.update { state -> if (state is BillDetailUiState.Content) transform(state) else state }
    }

    private fun updateSplit(transform: (BillSheet.Split) -> BillSheet.Split) = updateSheet<BillSheet.Split>(transform)

    private inline fun <reified S : BillSheet> updateSheet(crossinline transform: (S) -> S) {
        updateContent { content -> (content.sheet as? S)?.let { content.copy(sheet = transform(it)) } ?: content }
    }
}
