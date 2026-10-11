package app.rasoi.server.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object SettlementsTable : Table("settlements") {
    val id = uuid("id")
    val periodId = uuid("period_id").references(PeriodsTable.id)
    val fromUser = uuid("from_user").references(UsersTable.id)
    val toUser = uuid("to_user").references(UsersTable.id)
    val amountPaise = long("amount_paise")
    val status = text("status")
    val paidAt = timestamp("paid_at").nullable()
    override val primaryKey = PrimaryKey(id)
}
