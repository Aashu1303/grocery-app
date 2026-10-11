package app.rasoi.core.model

import kotlinx.datetime.LocalDate

/**
 * Something in the flat's kitchen. [expiresOn] is an estimate the household can edit; [sourceLineId] ties an item
 * to the bill line it came from, and manual items have none. [price] and [payerId] are kept for reference only.
 */
data class PantryItem(
    val id: PantryItemId,
    val householdId: HouseholdId,
    val name: String,
    val category: Category,
    val quantity: Quantity,
    val locationId: LocationId?,
    val expiresOn: LocalDate?,
    val status: PantryStatus,
    val addedOn: LocalDate,
    val price: Money? = null,
    val payerId: UserId? = null,
    val sourceLineId: BillLineId? = null,
)
