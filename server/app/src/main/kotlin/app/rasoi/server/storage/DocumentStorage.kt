package app.rasoi.server.storage

import app.rasoi.core.model.AppResult

/** Keeps uploaded invoice PDFs. Keys are opaque paths such as `households/<id>/<sha256>.pdf`. */
interface DocumentStorage {
    suspend fun put(key: String, bytes: ByteArray): AppResult<Unit>

    suspend fun get(key: String): ByteArray?

    suspend fun delete(key: String)
}
