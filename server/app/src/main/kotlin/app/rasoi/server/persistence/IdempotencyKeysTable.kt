package app.rasoi.server.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object IdempotencyKeysTable : Table("idempotency_keys") {
    val userId = uuid("user_id").references(UsersTable.id)
    val key = text("key")
    val requestHash = text("request_hash")
    val statusCode = integer("status_code")
    val responseBody = text("response_body")
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(userId, key)
}
