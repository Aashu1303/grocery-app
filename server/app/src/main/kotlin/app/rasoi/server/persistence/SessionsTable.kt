package app.rasoi.server.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object SessionsTable : Table("sessions") {
    val tokenHash = text("token_hash")
    val userId = uuid("user_id").references(UsersTable.id)
    val createdAt = timestamp("created_at")
    val expiresAt = timestamp("expires_at")
    override val primaryKey = PrimaryKey(tokenHash)
}
