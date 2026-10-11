package app.rasoi.server.persistence

import app.rasoi.core.model.UserId
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.time.Instant

class DeviceStore(private val db: RasoiDatabase) {
    /** Stores [token] for [userId]; a token registered before, even by someone else on the same phone, moves. */
    suspend fun register(userId: UserId, token: String, platform: String, now: Instant) = db.query {
        DeviceTokensTable.deleteWhere { DeviceTokensTable.token eq token }
        DeviceTokensTable.insert {
            it[DeviceTokensTable.token] = token
            it[DeviceTokensTable.userId] = userId.uuid()
            it[DeviceTokensTable.platform] = platform
            it[updatedAt] = now
        }
    }

    suspend fun tokensFor(users: Collection<UserId>): List<String> = db.query {
        if (users.isEmpty()) return@query emptyList()
        DeviceTokensTable.selectAll().where { DeviceTokensTable.userId inList users.map { it.uuid() } }
            .map { it[DeviceTokensTable.token] }
    }

    suspend fun remove(tokens: Collection<String>) {
        if (tokens.isEmpty()) return
        db.query { DeviceTokensTable.deleteWhere { DeviceTokensTable.token inList tokens.toList() } }
    }
}
