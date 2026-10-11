package app.rasoi.server.persistence

import org.jetbrains.exposed.v1.core.Table

internal object AllocationsTable : Table("allocations") {
    val lineId = uuid("line_id").references(BillLinesTable.id)
    val userId = uuid("user_id").references(UsersTable.id)
    val mode = text("mode")
    val units = integer("units")
    val amountPaise = long("amount_paise")
    override val primaryKey = PrimaryKey(lineId, userId)
}
