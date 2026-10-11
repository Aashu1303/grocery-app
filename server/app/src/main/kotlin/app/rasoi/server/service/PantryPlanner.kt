package app.rasoi.server.service

import app.rasoi.core.domain.pantry.ExpiryRules
import app.rasoi.core.domain.pantry.PantryRules
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillLine
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.PantryItemId
import app.rasoi.server.persistence.LocationStore
import kotlin.uuid.Uuid

/** Turns food bill lines into pantry items with a storage place and an expiry estimate (design-plan 6.4). */
class PantryPlanner(private val locations: LocationStore) {
    suspend fun itemsFor(bill: Bill, lines: List<BillLine> = bill.lines): List<PantryItem> {
        val places = locations.list(bill.householdId)
        return lines.filter { it.isFood }.map { line ->
            val kind = ExpiryRules.defaultLocation(line.displayName, line.category)
            PantryRules.fromLine(
                line = line,
                bill = bill,
                locationId = (places.firstOrNull { it.kind == kind } ?: places.firstOrNull())?.id,
                expiresOn = ExpiryRules.estimate(line.displayName, line.category, bill.invoiceDate),
                id = PantryItemId(Uuid.random().toString()),
            )
        }
    }
}
