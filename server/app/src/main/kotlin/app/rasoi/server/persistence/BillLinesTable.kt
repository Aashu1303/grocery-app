package app.rasoi.server.persistence

import org.jetbrains.exposed.v1.core.Table

internal object BillLinesTable : Table("bill_lines") {
    val id = uuid("id")
    val billId = uuid("bill_id").references(BillsTable.id)
    val position = integer("position")
    val rawName = text("raw_name")
    val displayName = text("display_name")
    val units = integer("units")
    val amountPaise = long("amount_paise")
    val measureAmount = long("measure_amount").nullable()
    val measureUnit = text("measure_unit").nullable()
    val mrpPaise = long("mrp_paise").nullable()
    val hsn = text("hsn").nullable()
    val upc = text("upc").nullable()
    val category = text("category").nullable()
    val isFood = bool("is_food").nullable()
    override val primaryKey = PrimaryKey(id)
}
