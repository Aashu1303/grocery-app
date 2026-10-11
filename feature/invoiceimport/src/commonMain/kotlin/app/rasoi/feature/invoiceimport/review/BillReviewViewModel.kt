package app.rasoi.feature.invoiceimport.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rasoi.core.domain.bills.BillEdit
import app.rasoi.core.domain.bills.BillRules
import app.rasoi.core.domain.bills.GetBillUseCase
import app.rasoi.core.domain.bills.LineEdit
import app.rasoi.core.domain.bills.UpdateBillUseCase
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillWarning
import app.rasoi.core.model.Money
import app.rasoi.feature.invoiceimport.billError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/**
 * Reviews an imported draft: shows what the server read, lets the user fix a line and accept the calculated
 * total. Every change is saved to the server straight away; the server is authoritative (design-plan principle 6).
 */
@KoinViewModel
class BillReviewViewModel(
    @InjectedParam private val billId: BillId,
    private val getBill: GetBillUseCase,
    private val updateBill: UpdateBillUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow<BillReviewUiState>(BillReviewUiState.Loading)
    val state: StateFlow<BillReviewUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun onEvent(event: BillReviewUiEvent) {
        when (event) {
            BillReviewUiEvent.Retry -> load()
            is BillReviewUiEvent.EditLine -> openEdit(event)
            is BillReviewUiEvent.NameChanged -> updateEdit {
                it.copy(name = event.value, nameError = null, error = null)
            }
            is BillReviewUiEvent.UnitsStepped -> updateEdit {
                it.copy(units = (it.units + event.delta).coerceIn(1, BillRules.MAX_UNITS), error = null)
            }
            is BillReviewUiEvent.AmountChanged ->
                updateEdit { it.copy(amountText = event.value, amountError = null, error = null) }
            is BillReviewUiEvent.FoodChanged -> updateEdit { it.copy(isFood = event.isFood, error = null) }
            BillReviewUiEvent.SaveLine -> saveLine()
            BillReviewUiEvent.DismissEdit -> updateContent { it.copy(edit = null) }
            BillReviewUiEvent.UseCalculatedTotal -> useCalculatedTotal()
        }
    }

    private fun load() {
        mutableState.value = BillReviewUiState.Loading
        viewModelScope.launch {
            mutableState.value = when (val result = getBill(billId)) {
                is AppResult.Success -> BillReviewUiState.Content(result.value)
                is AppResult.Failure -> BillReviewUiState.Error(result.error.billError())
            }
        }
    }

    private fun openEdit(event: BillReviewUiEvent.EditLine) = updateContent { content ->
        val line = content.bill.lines.firstOrNull { it.id == event.lineId } ?: return@updateContent content
        content.copy(
            edit = LineEditState(line.id, line.displayName, line.units, line.amount.toDecimalString(), line.isFood),
            error = null,
        )
    }

    private fun saveLine() {
        val content = mutableState.value as? BillReviewUiState.Content
        val edit = content?.edit
        if (content == null || edit == null || edit.isSaving) return
        val amount = Money.parse(edit.amountText.trim())?.takeUnless { it.isNegative }
        if (amount == null) {
            updateEdit { it.copy(amountError = LineFieldError.INVALID) }
            return
        }
        val lines = content.bill.lines.map { line ->
            if (line.id == edit.lineId) {
                LineEdit(line.id, edit.name, edit.units, amount, edit.isFood)
            } else {
                LineEdit(line.id, line.displayName, line.units, line.amount)
            }
        }
        updateEdit { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            when (val result = updateBill(billId, BillEdit(lines = lines))) {
                is AppResult.Success -> updateContent { it.copy(bill = result.value, edit = null) }
                is AppResult.Failure -> updateEdit { it.withFailure(result.error) }
            }
        }
    }

    private fun useCalculatedTotal() {
        val content = mutableState.value as? BillReviewUiState.Content ?: return
        val mismatch = content.warnings.filterIsInstance<BillWarning.TotalMismatch>().firstOrNull() ?: return
        if (content.isSaving) return
        updateContent { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            when (val result = updateBill(billId, BillEdit(total = mismatch.computed))) {
                is AppResult.Success -> updateContent { it.copy(bill = result.value, isSaving = false) }
                is AppResult.Failure -> updateContent { it.copy(isSaving = false, error = result.error.billError()) }
            }
        }
    }

    private fun LineEditState.withFailure(error: AppError): LineEditState {
        val violations = (error as? AppError.Validation)?.violations.orEmpty()
        val nameError = violations.firstOrNull { it.field.endsWith(".name") }?.let {
            if (it.code == "too_long") LineFieldError.TOO_LONG else LineFieldError.REQUIRED
        }
        val amountError = violations.firstOrNull { it.field.endsWith(".amount") }?.let { LineFieldError.INVALID }
        val shown = nameError ?: amountError
        return copy(
            isSaving = false,
            nameError = nameError,
            amountError = amountError,
            error = if (shown == null) error.billError() else null,
        )
    }

    private fun updateContent(transform: (BillReviewUiState.Content) -> BillReviewUiState.Content) {
        mutableState.update { state -> if (state is BillReviewUiState.Content) transform(state) else state }
    }

    private fun updateEdit(transform: (LineEditState) -> LineEditState) =
        updateContent { content -> content.edit?.let { content.copy(edit = transform(it)) } ?: content }
}
