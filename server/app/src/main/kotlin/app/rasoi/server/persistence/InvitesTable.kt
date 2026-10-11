package app.rasoi.server.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object InvitesTable : Table("invites") {
    val code = text("code")
    val householdId = uuid("household_id").references(HouseholdsTable.id)
    val role = text("role")
    val createdBy = uuid("created_by").references(UsersTable.id)
    val expiresAt = timestamp("expires_at")
    val acceptedBy = uuid("accepted_by").references(UsersTable.id).nullable()
    val acceptedAt = timestamp("accepted_at").nullable()
    override val primaryKey = PrimaryKey(code)
}
