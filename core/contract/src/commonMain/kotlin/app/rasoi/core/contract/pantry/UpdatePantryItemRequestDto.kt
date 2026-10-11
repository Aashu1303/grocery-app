package app.rasoi.core.contract.pantry

import app.rasoi.core.contract.bills.QuantityDto
import kotlinx.serialization.Serializable

/** Omitted fields stay unchanged; an expiry is removed with [clearExpiry]. */
@Serializable
data class UpdatePantryItemRequestDto(
    val name: String? = null,
    val quantity: QuantityDto? = null,
    val locationId: String? = null,
    val expiresOn: String? = null,
    val clearExpiry: Boolean = false,
    val status: PantryStatusDto? = null,
)
