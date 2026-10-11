package app.rasoi.feature.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rasoi.core.domain.household.HouseholdRepository
import app.rasoi.core.domain.household.Permission
import app.rasoi.core.domain.household.RolePolicy
import app.rasoi.core.domain.pantry.AddPantryItemUseCase
import app.rasoi.core.domain.pantry.Categorizer
import app.rasoi.core.domain.pantry.ListPantryUseCase
import app.rasoi.core.domain.pantry.NewPantryItem
import app.rasoi.core.domain.pantry.PantryEdit
import app.rasoi.core.domain.pantry.QuantityInput
import app.rasoi.core.domain.pantry.UpdatePantryItemUseCase
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.PantryStatus
import app.rasoi.core.model.Platform
import app.rasoi.core.model.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * What is in the flat's kitchen: add things by hand, move them between storage places, adjust the expiry estimate,
 * and mark them finished or discarded. Every change is saved to the server straight away.
 */
class PantryViewModel(
    private val householdId: HouseholdId,
    private val myRole: Role,
    private val households: HouseholdRepository,
    private val listPantry: ListPantryUseCase,
    private val addItem: AddPantryItemUseCase,
    private val updateItem: UpdatePantryItemUseCase,
    private val clock: Clock,
) : ViewModel() {
    private val mutableState = MutableStateFlow<PantryUiState>(PantryUiState.Loading)
    val state: StateFlow<PantryUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun onEvent(event: PantryUiEvent) {
        when (event) {
            PantryUiEvent.Retry -> load()
            is PantryUiEvent.FilterSelected -> updateContent { it.copy(filter = event.locationId) }
            PantryUiEvent.OpenAdd -> updateContent { if (it.canEdit) it.copy(sheet = PantrySheet.Add()) else it }
            PantryUiEvent.SaveNew -> saveNew()
            is PantryUiEvent.OpenItem -> updateContent { it.copy(sheet = PantrySheet.Details(event.itemId)) }
            PantryUiEvent.DismissSheet -> updateContent { it.copy(sheet = null) }
            is PantryUiEvent.Form -> editForm(event)
            is PantryUiEvent.Change -> applyChange(event)
        }
    }

    private fun editForm(event: PantryUiEvent.Form) = when (event) {
        is PantryUiEvent.Form.NameChanged -> updateForm { it.copy(name = event.value, nameError = null, error = null) }
        is PantryUiEvent.Form.AmountChanged ->
            updateForm { it.copy(amountText = event.value, amountError = null, error = null) }
        is PantryUiEvent.Form.UnitSelected -> updateForm { it.copy(unit = event.unit, amountError = null) }
        is PantryUiEvent.Form.LocationSelected -> updateForm { it.copy(locationId = event.locationId) }
    }

    private fun applyChange(event: PantryUiEvent.Change) = when (event) {
        is PantryUiEvent.Change.MoveTo -> change(PantryEdit(locationId = event.locationId))
        is PantryUiEvent.Change.ExpiryStepped -> stepExpiry(event.days)
        PantryUiEvent.Change.MarkFinished -> change(PantryEdit(status = PantryStatus.FINISHED))
        PantryUiEvent.Change.AskDiscard -> askDiscard()
        PantryUiEvent.Change.ConfirmDiscard -> discard()
    }

    private fun load() {
        mutableState.value = PantryUiState.Loading
        viewModelScope.launch {
            val details = households.details(householdId)
            val items = listPantry(householdId)
            mutableState.value = when {
                details is AppResult.Failure -> PantryUiState.Error(details.error.pantryError())
                items is AppResult.Failure -> PantryUiState.Error(items.error.pantryError())
                details is AppResult.Success && items is AppResult.Success -> PantryUiState.Content(
                    items = items.value.byExpiry(),
                    locations = details.value.locations,
                    today = clock.now().toLocalDateTime(details.value.household.timeZone).date,
                    canEdit = RolePolicy.allows(myRole, Permission.EDIT_PANTRY),
                )
                else -> PantryUiState.Error(PantryError.GENERIC)
            }
        }
    }

    private fun saveNew() {
        val form = ((mutableState.value as? PantryUiState.Content)?.sheet as? PantrySheet.Add)?.form
        if (form == null || form.isSaving) return
        val quantity = QuantityInput.parse(form.amountText, form.unit)
        if (quantity == null) {
            updateForm { it.copy(amountError = PantryFieldError.INVALID) }
            return
        }
        updateForm { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val category = Categorizer.categorize(form.name, hsn = null, platform = Platform.MANUAL).category
            val item = NewPantryItem(form.name, category, quantity, form.locationId, expiresOn = null)
            when (val result = addItem(householdId, myRole, item)) {
                is AppResult.Success -> updateContent {
                    it.copy(items = (it.items + result.value).byExpiry(), sheet = null)
                }
                is AppResult.Failure -> updateForm {
                    val nameError = result.error.fieldError("name")
                    val amountError = result.error.fieldError("quantity")
                    it.copy(
                        isSaving = false,
                        nameError = nameError,
                        amountError = amountError,
                        error = if (nameError == null && amountError == null) result.error.pantryError() else null,
                    )
                }
            }
        }
    }

    private fun stepExpiry(days: Int) {
        val content = mutableState.value as? PantryUiState.Content ?: return
        val item = content.openItem() ?: return
        val base = item.expiresOn ?: content.today
        change(PantryEdit(expiresOn = base.plus(DatePeriod(days = days))))
    }

    private fun askDiscard() = updateContent { content ->
        val details = content.sheet as? PantrySheet.Details
        if (details == null) content else content.copy(sheet = PantrySheet.ConfirmDiscard(details.itemId))
    }

    private fun discard() {
        val confirm = (mutableState.value as? PantryUiState.Content)?.sheet as? PantrySheet.ConfirmDiscard ?: return
        updateContent { it.copy(sheet = PantrySheet.Details(confirm.itemId)) }
        change(PantryEdit(status = PantryStatus.DISCARDED))
    }

    /** Saves [edit] to the item in the open details sheet; finished and discarded items leave the list. */
    private fun change(edit: PantryEdit) {
        val content = mutableState.value as? PantryUiState.Content ?: return
        val details = content.sheet as? PantrySheet.Details ?: return
        if (details.isSaving) return
        updateDetails { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            when (val result = updateItem(householdId, details.itemId, edit)) {
                is AppResult.Success -> updateContent { current ->
                    val others = current.items.filterNot { it.id == details.itemId }
                    val inStock = result.value.status == PantryStatus.IN_STOCK
                    current.copy(
                        items = if (inStock) (others + result.value).byExpiry() else others,
                        sheet = if (inStock) details.copy(isSaving = false) else null,
                    )
                }
                is AppResult.Failure -> updateDetails { it.copy(isSaving = false, error = result.error.pantryError()) }
            }
        }
    }

    private fun PantryUiState.Content.openItem(): PantryItem? {
        val id = (sheet as? PantrySheet.Details)?.itemId ?: return null
        return items.firstOrNull { it.id == id }
    }

    private fun List<PantryItem>.byExpiry(): List<PantryItem> = sortedWith(
        compareBy<PantryItem> {
            it.expiresOn == null
        }.thenBy { it.expiresOn }.thenBy { it.name.lowercase() },
    )

    private fun updateContent(transform: (PantryUiState.Content) -> PantryUiState.Content) {
        mutableState.update { state -> if (state is PantryUiState.Content) transform(state) else state }
    }

    private fun updateForm(transform: (AddItemState) -> AddItemState) = updateContent { content ->
        val add = content.sheet as? PantrySheet.Add
        if (add == null) content else content.copy(sheet = add.copy(form = transform(add.form)))
    }

    private fun updateDetails(transform: (PantrySheet.Details) -> PantrySheet.Details) = updateContent { content ->
        val details = content.sheet as? PantrySheet.Details
        if (details == null) content else content.copy(sheet = transform(details))
    }
}
