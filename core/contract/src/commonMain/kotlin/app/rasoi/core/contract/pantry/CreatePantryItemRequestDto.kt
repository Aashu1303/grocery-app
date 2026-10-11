package app.rasoi.core.contract.pantry

import app.rasoi.core.contract.bills.CategoryDto
import app.rasoi.core.contract.bills.QuantityDto
import kotlinx.serialization.Serializable

/** An item added by hand; an omitted [expiresOn] is estimated from the category. */
@Serializable
data class CreatePantryItemRequestDto(
    val name: String,
    val category: CategoryDto,
    val quantity: QuantityDto,
    val locationId: String? = null,
    val expiresOn: String? = null,
)
