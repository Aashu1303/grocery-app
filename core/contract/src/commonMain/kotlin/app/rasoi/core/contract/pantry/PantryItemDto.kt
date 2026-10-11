package app.rasoi.core.contract.pantry

import app.rasoi.core.contract.bills.CategoryDto
import app.rasoi.core.contract.bills.QuantityDto
import kotlinx.serialization.Serializable

/** Dates are ISO (`2026-10-09`). [expiresOn] is an editable estimate. */
@Serializable
data class PantryItemDto(
    val id: String,
    val name: String,
    val category: CategoryDto,
    val quantity: QuantityDto,
    val locationId: String? = null,
    val expiresOn: String? = null,
    val status: PantryStatusDto,
    val addedOn: String,
    val pricePaise: Long? = null,
    val sourceLineId: String? = null,
)
