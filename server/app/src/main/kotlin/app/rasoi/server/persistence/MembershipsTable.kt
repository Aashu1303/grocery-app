package app.rasoi.server.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object MembershipsTable : Table("memberships") {
    val householdId = uuid("household_id").references(HouseholdsTable.id)
    val userId = uuid("user_id").references(UsersTable.id)
    val role = text("role")
    val joinedAt = timestamp("joined_at")
    override val primaryKey = PrimaryKey(householdId, userId)
}
