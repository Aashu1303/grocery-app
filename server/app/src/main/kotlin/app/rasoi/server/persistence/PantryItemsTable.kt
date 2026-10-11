package app.rasoi.server.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestamp

internal object PantryItemsTable : Table("pantry_items") {
    val id = uuid("id")
    val householdId = uuid("household_id").references(HouseholdsTable.id)
    val name = text("name")
    val category = text("category")
    val quantityAmount = long("quantity_amount")
    val quantityUnit = text("quantity_unit")
    val locationId = uuid("location_id").references(LocationsTable.id).nullable()
    val expiresOn = date("expires_on").nullable()
    val status = text("status")
    val addedOn = date("added_on")
    val pricePaise = long("price_paise").nullable()
    val payerId = uuid("payer_id").references(UsersTable.id).nullable()
    val sourceLineId = uuid("source_line_id").references(BillLinesTable.id).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}
