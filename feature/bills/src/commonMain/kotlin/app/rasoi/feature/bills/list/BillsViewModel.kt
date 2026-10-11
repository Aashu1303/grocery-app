package app.rasoi.feature.bills.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rasoi.core.domain.bills.CreateManualBillUseCase
import app.rasoi.core.domain.bills.LineEdit
import app.rasoi.core.domain.bills.ListBillsUseCase
import app.rasoi.core.domain.bills.ManualBillDraft
import app.rasoi.core.domain.household.HouseholdRepository
import app.rasoi.core.domain.household.Permission
import app.rasoi.core.domain.household.RolePolicy
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Money
import app.rasoi.core.model.Role
import app.rasoi.feature.bills.FieldError
import app.rasoi.feature.bills.billsError
import app.rasoi.feature.bills.fieldError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The household's bills, newest first, with entry points for importing an invoice or typing a bill in. */
class BillsViewModel(
    private val householdId: HouseholdId,
    private val myRole: Role,
    private val households: HouseholdRepository,
    private val listBills: ListBillsUseCase,
    private val createBill: CreateManualBillUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow<BillsUiState>(BillsUiState.Loading)
    val state: StateFlow<BillsUiState> = mutableState.asStateFlow()

    private val effectChannel = Channel<BillsEffect>(Channel.BUFFERED)
    val effects: Flow<BillsEffect> = effectChannel.receiveAsFlow()

    init {
        load(showLoading = true)
    }

    fun onEvent(event: BillsUiEvent) {
        when (event) {
            BillsUiEvent.Retry -> load(showLoading = true)
            BillsUiEvent.Refresh -> if (mutableState.value != BillsUiState.Loading) load(showLoading = false)
            is BillsUiEvent.OpenBill -> send(BillsEffect.OpenBill(event.billId))
            BillsUiEvent.ImportInvoice -> send(BillsEffect.OpenImport)
            BillsUiEvent.OpenNew -> updateContent { if (it.canEdit) it.copy(form = NewBillForm()) else it }
            BillsUiEvent.DismissNew -> updateContent { it.copy(form = null) }
            BillsUiEvent.SaveNew -> saveNew()
            is BillsUiEvent.TitleChanged ->
                updateForm { it.copy(title = event.value, titleError = null, error = null) }
            is BillsUiEvent.AmountChanged ->
                updateForm { it.copy(amountText = event.value, amountError = null, error = null) }
        }
    }

    private fun load(showLoading: Boolean) {
        if (showLoading) mutableState.value = BillsUiState.Loading
        viewModelScope.launch {
            val details = households.details(householdId)
            val bills = listBills(householdId, myRole)
            if (details is AppResult.Success && bills is AppResult.Success) {
                val names = details.value.members.associate { it.userId to it.displayName }
                val canEdit = RolePolicy.allows(myRole, Permission.EDIT_BILLS)
                mutableState.update { previous ->
                    val form = (previous as? BillsUiState.Content)?.form
                    BillsUiState.Content(bills.value, names, canEdit, form)
                }
            } else {
                val error = (bills as? AppResult.Failure)?.error ?: (details as AppResult.Failure).error
                if (showLoading) mutableState.value = BillsUiState.Error(error.billsError())
            }
        }
    }

    private fun saveNew() {
        val content = mutableState.value as? BillsUiState.Content
        val form = content?.form
        if (content == null || form == null || form.isSaving) return
        val amount = Money.parse(form.amountText.trim())?.takeUnless { it.isNegative || it.isZero }
        val titleError = if (form.title.isBlank()) FieldError.REQUIRED else null
        val amountError = if (amount == null) FieldError.INVALID else null
        if (amount == null || titleError != null) {
            updateForm { it.copy(titleError = titleError, amountError = amountError) }
            return
        }
        updateForm { it.copy(isSaving = true, error = null) }
        val draft = ManualBillDraft(
            title = form.title,
            lines = listOf(LineEdit(null, form.title.trim(), 1, amount)),
            charges = emptyList(),
        )
        viewModelScope.launch {
            when (val result = createBill(householdId, myRole, draft)) {
                is AppResult.Success -> {
                    updateContent { it.copy(form = null) }
                    send(BillsEffect.OpenBill(result.value.id))
                }
                is AppResult.Failure -> updateForm { it.withFailure(result.error) }
            }
        }
    }

    private fun NewBillForm.withFailure(error: AppError): NewBillForm {
        val titleError = error.fieldError("title")
        return copy(
            isSaving = false,
            titleError = titleError,
            error = if (titleError == null) error.billsError() else null,
        )
    }

    private fun send(effect: BillsEffect) {
        viewModelScope.launch { effectChannel.send(effect) }
    }

    private fun updateContent(transform: (BillsUiState.Content) -> BillsUiState.Content) {
        mutableState.update { state -> if (state is BillsUiState.Content) transform(state) else state }
    }

    private fun updateForm(transform: (NewBillForm) -> NewBillForm) =
        updateContent { content -> content.form?.let { content.copy(form = transform(it)) } ?: content }
}
