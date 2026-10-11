package app.rasoi.core.domain.kitchen

import app.rasoi.core.domain.pantry.ExpiryRules
import app.rasoi.core.model.Freshness
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.PantryStatus
import kotlinx.datetime.LocalDate

/** An item to cook soon, with how many days it has left (negative once expired, null without a date). */
data class UseFirstEntry(val item: PantryItem, val daysLeft: Int?, val freshness: Freshness)

/**
 * Design-plan 6.4 item 8: food still in stock, soonest expiry first, then the smaller quantity (easier to finish),
 * then by name so the order is stable. Items without an expiry date come last.
 */
object UseFirst {
    fun rank(items: List<PantryItem>, today: LocalDate): List<UseFirstEntry> = items
        .filter { it.status == PantryStatus.IN_STOCK && it.category.isFood }
        .map { item ->
            val expiry = item.expiresOn
            UseFirstEntry(
                item = item,
                daysLeft = expiry?.let { ExpiryRules.daysLeft(it, today) },
                freshness = expiry?.let { ExpiryRules.freshness(it, today) } ?: Freshness.FRESH,
            )
        }
        .sortedWith(
            compareBy<UseFirstEntry, Int?>(nullsLast()) { it.daysLeft }
                .thenBy { it.item.quantity.amount }
                .thenBy { it.item.name },
        )
}
