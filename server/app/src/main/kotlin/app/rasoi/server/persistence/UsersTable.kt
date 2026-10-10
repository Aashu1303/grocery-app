package app.rasoi.server.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object UsersTable : Table("users") {
    val id = uuid("id")
    val firebaseUid = text("firebase_uid")
    val phone = text("phone")
    val displayName = text("display_name")
    val upiVpa = text("upi_vpa").nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
