package app.rasoi.server.persistence

import org.jetbrains.exposed.v1.core.Table

internal object BillChargesTable : Table("bill_charges") {
    val id = uuid("id")
    val billId = uuid("bill_id").references(BillsTable.id)
    val position = integer("position")
    val kind = text("kind")
    val label = text("label")
    val amountPaise = long("amount_paise")
    override val primaryKey = PrimaryKey(id)
}
