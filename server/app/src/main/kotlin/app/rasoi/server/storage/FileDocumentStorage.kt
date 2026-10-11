package app.rasoi.server.storage

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** Stores documents under [root]; writes go to a temporary file first so a crash never leaves half a PDF. */
class FileDocumentStorage(private val root: Path) : DocumentStorage {
    override suspend fun put(key: String, bytes: ByteArray): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val target = resolve(key)
            Files.createDirectories(target.parent)
            val temporary = Files.createTempFile(target.parent, "upload", ".tmp")
            Files.write(temporary, bytes)
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            AppResult.Success(Unit)
        } catch (_: IOException) {
            AppResult.Failure(AppError.Unexpected("storage_unavailable"))
        }
    }

    override suspend fun get(key: String): ByteArray? = withContext(Dispatchers.IO) {
        resolve(key).takeIf(Files::exists)?.let(Files::readAllBytes)
    }

    override suspend fun delete(key: String) {
        withContext(Dispatchers.IO) { Files.deleteIfExists(resolve(key)) }
    }

    private fun resolve(key: String): Path {
        val path = root.resolve(key).normalize()
        require(path.startsWith(root.normalize())) { "Storage key escapes the root" }
        return path
    }
}
