package app.rasoi.server.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object PeriodsTable : Table("periods") {
    val id = uuid("id")
    val householdId = uuid("household_id").references(HouseholdsTable.id)
    val month = text("month")
    val status = text("status")
    val createdAt = timestamp("created_at")
    val closedAt = timestamp("closed_at").nullable()
    override val primaryKey = PrimaryKey(id)
}
