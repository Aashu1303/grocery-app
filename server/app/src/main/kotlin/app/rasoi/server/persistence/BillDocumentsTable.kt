package app.rasoi.server.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object BillDocumentsTable : Table("bill_documents") {
    val id = uuid("id")
    val billId = uuid("bill_id").references(BillsTable.id)
    val householdId = uuid("household_id").references(HouseholdsTable.id)
    val platform = text("platform")
    val invoiceNo = text("invoice_no")
    val storageKey = text("storage_key")
    val sha256 = text("sha256")
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
