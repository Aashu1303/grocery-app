package app.rasoi.core.domain.pantry

import app.rasoi.core.model.Category
import app.rasoi.core.model.LocationId
import app.rasoi.core.model.Quantity
import kotlinx.datetime.LocalDate

/** An item added by hand. */
data class NewPantryItem(
    val name: String,
    val category: Category,
    val quantity: Quantity,
    val locationId: LocationId?,
    val expiresOn: LocalDate?,
)
