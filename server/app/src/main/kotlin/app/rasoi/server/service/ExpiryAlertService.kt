package app.rasoi.server.service

import app.rasoi.core.domain.kitchen.ExpiryAlert
import app.rasoi.core.domain.kitchen.ExpiryAlertKind
import app.rasoi.core.domain.kitchen.ExpiryAlerts
import app.rasoi.core.domain.ledger.PeriodRules
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.PantryItem
import app.rasoi.server.notifications.PushMessage
import app.rasoi.server.persistence.AlertStore
import app.rasoi.server.persistence.HouseholdStore
import app.rasoi.server.persistence.PantryStore
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlin.time.Clock

/**
 * The sweep that tells a household about food that is about to expire or already has (design-plan 6.4 item 7). It
 * derives what is due from the pantry and the clock and remembers what it sent, so running twice sends nothing twice.
 */
class ExpiryAlertService(
    private val pantry: PantryStore,
    private val alerts: AlertStore,
    private val householdStore: HouseholdStore,
    private val notifications: NotificationService,
    private val clock: Clock,
) {
    /** Sends what is due; returns how many items were alerted. */
    suspend fun sendDue(): Int {
        val now = clock.now()
        // Any household's today is at most a day away from UTC's, so this covers the alert window everywhere.
        val horizon = PeriodRules.today(now, TimeZone.UTC).plus(DatePeriod(days = LOOKAHEAD_DAYS))
        val byHousehold = pantry.expiringUntil(horizon).groupBy { (item, _) -> item.householdId }
        return byHousehold.entries.sumOf { (householdId, rows) ->
            alertHousehold(householdId, rows.map { it.first }, rows.first().second)
        }
    }

    private suspend fun alertHousehold(householdId: HouseholdId, items: List<PantryItem>, zone: TimeZone): Int {
        val today = PeriodRules.today(clock.now(), zone)
        val due = ExpiryAlerts.due(items, today, alerts.sentFor(items.map { it.id }))
        if (due.isEmpty()) return 0
        val members = householdStore.members(householdId).map { it.userId }
        due.groupBy { it.kind }.forEach { (kind, group) -> notifications.notify(members, message(kind, group)) }
        alerts.markSent(due.map { it.item.id to it.kind }, clock.now())
        return due.size
    }

    private fun message(kind: ExpiryAlertKind, group: List<ExpiryAlert>): PushMessage {
        val names = group.map { it.item.name }
        val shown =
            names.take(SHOWN_NAMES).joinToString() +
                if (names.size > SHOWN_NAMES) " and ${names.size - SHOWN_NAMES} more" else ""
        return when (kind) {
            ExpiryAlertKind.EXPIRES_SOON -> PushMessage("Use soon", shown, mapOf("type" to "expires_soon"))
            ExpiryAlertKind.EXPIRED -> PushMessage("Expired", shown, mapOf("type" to "expired"))
        }
    }

    private companion object {
        const val LOOKAHEAD_DAYS = 3
        const val SHOWN_NAMES = 3
    }
}
