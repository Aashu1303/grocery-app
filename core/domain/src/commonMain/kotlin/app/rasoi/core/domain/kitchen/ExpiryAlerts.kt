package app.rasoi.core.domain.kitchen

import app.rasoi.core.domain.pantry.ExpiryRules
import app.rasoi.core.model.Freshness
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.PantryItemId
import app.rasoi.core.model.PantryStatus
import kotlinx.datetime.LocalDate

enum class ExpiryAlertKind { EXPIRES_SOON, EXPIRED }

data class ExpiryAlert(val item: PantryItem, val kind: ExpiryAlertKind, val daysLeft: Int)

/** Decides which expiry alerts to send now; the server remembers what it sent, so each kind goes out once per item. */
object ExpiryAlerts {
    fun due(
        items: List<PantryItem>,
        today: LocalDate,
        sent: Set<Pair<PantryItemId, ExpiryAlertKind>>,
    ): List<ExpiryAlert> = items.filter { it.status == PantryStatus.IN_STOCK && it.category.isFood }
        .mapNotNull { item ->
            val expiry = item.expiresOn ?: return@mapNotNull null
            val kind = when (ExpiryRules.freshness(expiry, today)) {
                Freshness.EXPIRED -> ExpiryAlertKind.EXPIRED
                Freshness.USE_SOON -> ExpiryAlertKind.EXPIRES_SOON
                Freshness.FRESH -> return@mapNotNull null
            }
            ExpiryAlert(item, kind, ExpiryRules.daysLeft(expiry, today)).takeIf { item.id to kind !in sent }
        }
}
