package app.rasoi.server.persistence

import app.rasoi.core.domain.kitchen.ExpiryAlertKind
import app.rasoi.core.model.PantryItemId
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** Remembers which expiry alerts went out, so each kind is sent once per item. */
class AlertStore(private val db: RasoiDatabase) {
    suspend fun sentFor(itemIds: Collection<PantryItemId>): Set<Pair<PantryItemId, ExpiryAlertKind>> = db.query {
        if (itemIds.isEmpty()) return@query emptySet()
        ExpiryAlertsSentTable.selectAll()
            .where { ExpiryAlertsSentTable.itemId inList itemIds.map { Uuid.parse(it.value) } }
            .map {
                PantryItemId(it[ExpiryAlertsSentTable.itemId].toString()) to
                    ExpiryAlertKind.valueOf(it[ExpiryAlertsSentTable.kind])
            }
            .toSet()
    }

    suspend fun markSent(alerts: Collection<Pair<PantryItemId, ExpiryAlertKind>>, now: Instant) {
        if (alerts.isEmpty()) return
        db.query {
            ExpiryAlertsSentTable.batchInsert(alerts, ignore = true) { (item, kind) ->
                this[ExpiryAlertsSentTable.itemId] = Uuid.parse(item.value)
                this[ExpiryAlertsSentTable.kind] = kind.name
                this[ExpiryAlertsSentTable.sentAt] = now
            }
        }
    }
}
