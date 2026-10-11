package app.rasoi.server.persistence

import app.rasoi.core.model.UserId
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.time.Instant

/** A response previously sent for an idempotency key. */
data class StoredResponse(val requestHash: String, val statusCode: Int, val body: String)

class IdempotencyStore(private val db: RasoiDatabase) {
    suspend fun find(userId: UserId, key: String): StoredResponse? = db.query {
        IdempotencyKeysTable.selectAll()
            .where { (IdempotencyKeysTable.userId eq userId.uuid()) and (IdempotencyKeysTable.key eq key) }
            .singleOrNull()
            ?.let {
                StoredResponse(
                    it[IdempotencyKeysTable.requestHash],
                    it[IdempotencyKeysTable.statusCode],
                    it[IdempotencyKeysTable.responseBody],
                )
            }
    }

    suspend fun save(userId: UserId, key: String, response: StoredResponse, now: Instant) = db.query {
        IdempotencyKeysTable.insertIgnore {
            it[IdempotencyKeysTable.userId] = userId.uuid()
            it[IdempotencyKeysTable.key] = key
            it[requestHash] = response.requestHash
            it[statusCode] = response.statusCode
            it[responseBody] = response.body
            it[createdAt] = now
        }
        Unit
    }

    suspend fun deleteOlderThan(cutoff: Instant): Int = db.query {
        IdempotencyKeysTable.deleteWhere { IdempotencyKeysTable.createdAt lessEq cutoff }
    }
}
