package app.rasoi.core.domain.pantry

import app.rasoi.core.model.LocationId
import app.rasoi.core.model.PantryStatus
import app.rasoi.core.model.Quantity
import kotlinx.datetime.LocalDate

/** A change to a pantry item; `null` leaves a field unchanged. An expiry is removed with [clearExpiry]. */
data class PantryEdit(
    val name: String? = null,
    val quantity: Quantity? = null,
    val locationId: LocationId? = null,
    val expiresOn: LocalDate? = null,
    val clearExpiry: Boolean = false,
    val status: PantryStatus? = null,
)
