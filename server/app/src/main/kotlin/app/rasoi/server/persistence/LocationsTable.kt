package app.rasoi.server.persistence

import org.jetbrains.exposed.v1.core.Table

internal object LocationsTable : Table("locations") {
    val id = uuid("id")
    val householdId = uuid("household_id").references(HouseholdsTable.id)
    val name = text("name")
    val kind = text("kind")
    override val primaryKey = PrimaryKey(id)
}
