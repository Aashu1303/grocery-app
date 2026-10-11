package app.rasoi.server.persistence

import app.rasoi.core.model.PhoneNumber
import app.rasoi.core.model.User
import app.rasoi.core.model.UserId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Instant
import kotlin.uuid.Uuid

class UserStore(private val db: RasoiDatabase) {
    suspend fun findByFirebaseUid(firebaseUid: String): User? = db.query {
        UsersTable.selectAll().where { UsersTable.firebaseUid eq firebaseUid }.singleOrNull()?.toUser()
    }

    suspend fun create(firebaseUid: String, phone: PhoneNumber, displayName: String, now: Instant): User = db.query {
        val id = Uuid.random()
        UsersTable.insert {
            it[UsersTable.id] = id
            it[UsersTable.firebaseUid] = firebaseUid
            it[UsersTable.phone] = phone.e164
            it[UsersTable.displayName] = displayName
            it[createdAt] = now
        }
        User(id.toUserId(), phone, displayName)
    }

    suspend fun setUpiVpa(id: UserId, vpa: String?) {
        db.query { UsersTable.update({ UsersTable.id eq id.uuid() }) { it[upiVpa] = vpa } }
    }

    suspend fun find(id: UserId): User? = db.query {
        UsersTable.selectAll().where { UsersTable.id eq id.uuid() }.singleOrNull()?.toUser()
    }
}

internal fun ResultRow.toUser(): User = User(
    id = this[UsersTable.id].toUserId(),
    phone = requireNotNull(PhoneNumber.parse(this[UsersTable.phone])) { "Stored phone is not E.164" },
    displayName = this[UsersTable.displayName],
    upiVpa = this[UsersTable.upiVpa],
)
