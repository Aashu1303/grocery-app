package app.rasoi.server.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestamp

internal object BillsTable : Table("bills") {
    val id = uuid("id")
    val householdId = uuid("household_id").references(HouseholdsTable.id)
    val platform = text("platform")
    val invoiceNo = text("invoice_no")
    val orderId = text("order_id").nullable()
    val invoiceDate = date("invoice_date")
    val payerId = uuid("payer_id").references(UsersTable.id)
    val createdBy = uuid("created_by").references(UsersTable.id)
    val status = text("status")
    val confidence = text("confidence")
    val totalPaise = long("total_paise")
    val submittedAt = timestamp("submitted_at").nullable()
    val title = text("title").nullable()
    val version = integer("version")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}
