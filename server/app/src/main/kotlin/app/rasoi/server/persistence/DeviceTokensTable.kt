package app.rasoi.server.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object DeviceTokensTable : Table("device_tokens") {
    val token = text("token")
    val userId = uuid("user_id").references(UsersTable.id)
    val platform = text("platform")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(token)
}
