package app.rasoi.feature.pantry

import app.rasoi.core.domain.pantry.InputUnit
import app.rasoi.core.model.LocationId
import app.rasoi.core.model.PantryItemId

sealed interface PantryUiEvent {
    data object Retry : PantryUiEvent

    data class FilterSelected(val locationId: LocationId?) : PantryUiEvent

    data object OpenAdd : PantryUiEvent

    data object SaveNew : PantryUiEvent

    data class OpenItem(val itemId: PantryItemId) : PantryUiEvent

    data object DismissSheet : PantryUiEvent

    /** Edits to the add form. */
    sealed interface Form : PantryUiEvent {
        data class NameChanged(val value: String) : Form

        data class AmountChanged(val value: String) : Form

        data class UnitSelected(val unit: InputUnit) : Form

        data class LocationSelected(val locationId: LocationId) : Form
    }

    /** Changes to the item in the open details sheet. */
    sealed interface Change : PantryUiEvent {
        data class MoveTo(val locationId: LocationId) : Change

        data class ExpiryStepped(val days: Int) : Change

        data object MarkFinished : Change

        data object AskDiscard : Change

        data object ConfirmDiscard : Change
    }
}
