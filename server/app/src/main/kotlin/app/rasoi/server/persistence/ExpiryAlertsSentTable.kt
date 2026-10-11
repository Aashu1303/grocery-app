package app.rasoi.server.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object ExpiryAlertsSentTable : Table("expiry_alerts_sent") {
    val itemId = uuid("item_id").references(PantryItemsTable.id)
    val kind = text("kind")
    val sentAt = timestamp("sent_at")
    override val primaryKey = PrimaryKey(itemId, kind)
}
