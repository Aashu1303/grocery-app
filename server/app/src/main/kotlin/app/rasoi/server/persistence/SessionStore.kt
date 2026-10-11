package app.rasoi.server.persistence

import app.rasoi.core.model.UserId
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.time.Instant

class SessionStore(private val db: RasoiDatabase) {
    suspend fun create(tokenHash: String, userId: UserId, now: Instant, expiresAt: Instant) = db.query {
        SessionsTable.insert {
            it[SessionsTable.tokenHash] = tokenHash
            it[SessionsTable.userId] = userId.uuid()
            it[createdAt] = now
            it[SessionsTable.expiresAt] = expiresAt
        }
        Unit
    }

    suspend fun userFor(tokenHash: String, now: Instant): UserId? = db.query {
        SessionsTable.selectAll()
            .where { (SessionsTable.tokenHash eq tokenHash) and (SessionsTable.expiresAt greater now) }
            .singleOrNull()
            ?.get(SessionsTable.userId)
            ?.toUserId()
    }

    suspend fun deleteExpired(now: Instant): Int = db.query {
        SessionsTable.deleteWhere { SessionsTable.expiresAt lessEq now }
    }
}
