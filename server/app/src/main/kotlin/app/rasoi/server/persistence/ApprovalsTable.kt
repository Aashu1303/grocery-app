package app.rasoi.server.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object ApprovalsTable : Table("approvals") {
    val billId = uuid("bill_id").references(BillsTable.id)
    val userId = uuid("user_id").references(UsersTable.id)
    val state = text("state")
    val comment = text("comment").nullable()
    val decidedAt = timestamp("decided_at").nullable()
    val automatic = bool("automatic")
    override val primaryKey = PrimaryKey(billId, userId)
}
