package app.rasoi.server.support

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.server.storage.DocumentStorage
import java.util.concurrent.ConcurrentHashMap

class InMemoryDocumentStorage(var failWrites: Boolean = false) : DocumentStorage {
    val files = ConcurrentHashMap<String, ByteArray>()

    override suspend fun put(key: String, bytes: ByteArray): AppResult<Unit> {
        if (failWrites) return AppResult.Failure(AppError.Unexpected("storage_unavailable"))
        files[key] = bytes
        return AppResult.Success(Unit)
    }

    override suspend fun get(key: String): ByteArray? = files[key]

    override suspend fun delete(key: String) {
        files.remove(key)
    }
}
