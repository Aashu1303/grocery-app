package app.rasoi.core.contract.kitchen

import app.rasoi.core.contract.pantry.PantryItemDto
import kotlinx.serialization.Serializable

/** An item to cook soon; [daysLeft] is negative once expired and null when the item has no date. */
@Serializable
data class UseFirstEntryDto(val item: PantryItemDto, val daysLeft: Int? = null, val freshness: FreshnessDto)
