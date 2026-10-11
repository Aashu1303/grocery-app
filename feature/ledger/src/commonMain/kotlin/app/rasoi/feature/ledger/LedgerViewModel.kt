package app.rasoi.feature.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rasoi.core.domain.household.HouseholdRepository
import app.rasoi.core.domain.ledger.CloseMonthUseCase
import app.rasoi.core.domain.ledger.GetLedgerUseCase
import app.rasoi.core.domain.ledger.LedgerRepository
import app.rasoi.core.domain.ledger.MarkPaidUseCase
import app.rasoi.core.domain.ledger.UpdateUpiUseCase
import app.rasoi.core.domain.ledger.UpiLink
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.PeriodMonth
import app.rasoi.core.model.Role
import app.rasoi.core.model.SettlementId
import app.rasoi.core.model.UserId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Who owes whom this month, the transfers that settle it, and paying, marking paid or closing the month. */
class LedgerViewModel(
    private val householdId: HouseholdId,
    private val me: UserId,
    private val myRole: Role,
    private val households: HouseholdRepository,
    private val ledgers: LedgerRepository,
    private val getLedger: GetLedgerUseCase,
    private val closeMonth: CloseMonthUseCase,
    private val markPaid: MarkPaidUseCase,
    private val updateUpi: UpdateUpiUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow<LedgerUiState>(LedgerUiState.Loading)
    val state: StateFlow<LedgerUiState> = mutableState.asStateFlow()

    private val effectChannel = Channel<LedgerEffect>(Channel.BUFFERED)
    val effects: Flow<LedgerEffect> = effectChannel.receiveAsFlow()

    init {
        load(month = null, showLoading = true)
    }

    fun onEvent(event: LedgerUiEvent) {
        when (event) {
            LedgerUiEvent.Retry -> load(shown(), showLoading = true)
            LedgerUiEvent.Refresh -> if (mutableState.value != LedgerUiState.Loading) load(shown(), false)
            is LedgerUiEvent.SelectMonth -> load(event.month, showLoading = false)
            is LedgerUiEvent.PayWithUpi -> payWithUpi(event.settlementId)
            is LedgerUiEvent.MarkPaid -> markTransferPaid(event.settlementId)
            LedgerUiEvent.AskClose -> updateContent {
                if (it.canClose) it.copy(sheet = LedgerSheet.ConfirmClose) else it
            }
            LedgerUiEvent.ConfirmClose -> close()
            LedgerUiEvent.DismissSheet -> updateContent { it.copy(sheet = null, error = null) }
            is LedgerUiEvent.Upi -> onUpi(event)
        }
    }

    private fun onUpi(event: LedgerUiEvent.Upi) = when (event) {
        LedgerUiEvent.Upi.Edit -> updateContent { it.copy(sheet = LedgerSheet.Upi(it.myUpi.orEmpty()), error = null) }
        is LedgerUiEvent.Upi.Changed -> updateContent { it.copy(sheet = LedgerSheet.Upi(event.value), error = null) }
        LedgerUiEvent.Upi.Save -> saveUpi()
        LedgerUiEvent.Upi.Unavailable -> updateContent { it.copy(error = LedgerError.NO_UPI_APP) }
    }

    private fun shown(): PeriodMonth? = (mutableState.value as? LedgerUiState.Content)?.ledger?.month

    private fun load(month: PeriodMonth?, showLoading: Boolean) {
        if (showLoading) mutableState.value = LedgerUiState.Loading
        viewModelScope.launch {
            val result = getLedger(householdId, myRole, month)
            val details = households.details(householdId)
            val upi = (ledgers.myUpiVpa() as? AppResult.Success)?.value
            if (result is AppResult.Success && details is AppResult.Success) {
                val names = details.value.members.associate { it.userId to it.displayName }
                mutableState.update { previous ->
                    val sheet = (previous as? LedgerUiState.Content)?.sheet
                    LedgerUiState.Content(result.value, names, me, myRole, upi, sheet)
                }
            } else if (showLoading) {
                val error = (result as? AppResult.Failure)?.error ?: (details as AppResult.Failure).error
                mutableState.value = LedgerUiState.Error(error.ledgerError())
            }
        }
    }

    private fun payWithUpi(settlementId: SettlementId) {
        val content = mutableState.value as? LedgerUiState.Content ?: return
        val item = content.ledger.transfers.firstOrNull { it.settlementId == settlementId }
        val payee = item?.let { content.names[it.transfer.to] }.orEmpty()
        val link = item?.takeIf(content::canPayByUpi)?.let {
            UpiLink.build(it.payeeUpiVpa.orEmpty(), payee, it.transfer.amount, "Rasoi ${content.ledger.month}")
        }
        if (link != null) viewModelScope.launch { effectChannel.send(LedgerEffect.OpenUpi(link)) }
    }

    private fun markTransferPaid(settlementId: SettlementId) {
        val content = mutableState.value as? LedgerUiState.Content ?: return
        val item = content.ledger.transfers.firstOrNull { it.settlementId == settlementId } ?: return
        if (content.isBusy || !content.canMarkPaid(item)) return
        perform {
            when (val result = markPaid(settlementId)) {
                is AppResult.Success -> {
                    reloadShown()
                    null
                }
                is AppResult.Failure -> result.error
            }
        }
    }

    private fun close() {
        val content = mutableState.value as? LedgerUiState.Content ?: return
        if (content.isBusy || !content.canClose) return
        perform {
            when (val result = closeMonth(householdId, myRole, content.ledger.month)) {
                is AppResult.Success -> {
                    updateContent { it.copy(ledger = result.value, sheet = null) }
                    null
                }
                is AppResult.Failure -> result.error
            }
        }
    }

    private fun saveUpi() {
        val content = mutableState.value as? LedgerUiState.Content ?: return
        val sheet = content.sheet as? LedgerSheet.Upi ?: return
        if (content.isBusy) return
        updateContent { it.copy(isBusy = true) }
        viewModelScope.launch {
            when (val result = updateUpi(sheet.text)) {
                is AppResult.Success -> updateContent { it.copy(myUpi = result.value, sheet = null, isBusy = false) }
                is AppResult.Failure -> updateContent {
                    val invalid = result.error is AppError.Validation
                    it.copy(
                        isBusy = false,
                        sheet = if (invalid) sheet.copy(invalid = true) else sheet,
                        error = if (invalid) null else result.error.ledgerError(),
                    )
                }
            }
        }
    }

    /** Runs one change; a failure is shown, and a closed-month conflict also reloads what is on screen. */
    private fun perform(block: suspend () -> AppError?) {
        updateContent { it.copy(isBusy = true, error = null) }
        viewModelScope.launch {
            val failure = block()
            updateContent { it.copy(isBusy = false, error = failure?.ledgerError()) }
            if (failure?.ledgerError() == LedgerError.CHANGED) reloadShown(null)
        }
    }

    private suspend fun reloadShown(month: PeriodMonth? = shown()) {
        (getLedger(householdId, myRole, month) as? AppResult.Success)?.let { fresh ->
            updateContent { it.copy(ledger = fresh.value) }
        }
    }

    private fun updateContent(transform: (LedgerUiState.Content) -> LedgerUiState.Content) {
        mutableState.update { state -> if (state is LedgerUiState.Content) transform(state) else state }
    }
}
