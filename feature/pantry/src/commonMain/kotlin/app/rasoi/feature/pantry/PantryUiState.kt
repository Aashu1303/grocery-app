package app.rasoi.feature.pantry

import app.rasoi.core.domain.pantry.InputUnit
import app.rasoi.core.model.Location
import app.rasoi.core.model.LocationId
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.PantryItemId
import kotlinx.datetime.LocalDate

sealed interface PantryUiState {
    data object Loading : PantryUiState

    data class Error(val error: PantryError) : PantryUiState

    /** [items] are in stock, soonest expiry first; [filter] limits them to one storage place. */
    data class Content(
        val items: List<PantryItem>,
        val locations: List<Location>,
        val today: LocalDate,
        val canEdit: Boolean,
        val filter: LocationId? = null,
        val sheet: PantrySheet? = null,
    ) : PantryUiState {
        val visibleItems: List<PantryItem> get() = items.filter { filter == null || it.locationId == filter }
    }
}

sealed interface PantrySheet {
    data class Add(val form: AddItemState = AddItemState()) : PantrySheet

    /** Actions for one item; [error] shows a failed change without closing the sheet. */
    data class Details(val itemId: PantryItemId, val isSaving: Boolean = false, val error: PantryError? = null) :
        PantrySheet

    data class ConfirmDiscard(val itemId: PantryItemId) : PantrySheet
}

data class AddItemState(
    val name: String = "",
    val amountText: String = "1",
    val unit: InputUnit = InputUnit.PIECES,
    val locationId: LocationId? = null,
    val nameError: PantryFieldError? = null,
    val amountError: PantryFieldError? = null,
    val error: PantryError? = null,
    val isSaving: Boolean = false,
)
