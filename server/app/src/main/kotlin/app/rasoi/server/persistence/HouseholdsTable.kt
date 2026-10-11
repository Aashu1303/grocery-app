package app.rasoi.server.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object HouseholdsTable : Table("households") {
    val id = uuid("id")
    val name = text("name")
    val timeZone = text("time_zone")
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
