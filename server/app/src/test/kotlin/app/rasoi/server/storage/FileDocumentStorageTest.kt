package app.rasoi.server.storage

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class FileDocumentStorageTest {
    private val root = Files.createTempDirectory("rasoi-storage")
    private val storage = FileDocumentStorage(root)

    @Test
    fun writesReadsAndDeletesUnderTheRoot() = runTest {
        assertEquals(AppResult.Success(Unit), storage.put("households/h1/abc.pdf", byteArrayOf(1, 2, 3)))
        assertContentEquals(byteArrayOf(1, 2, 3), storage.get("households/h1/abc.pdf"))
        assertEquals(
            listOf("abc.pdf"),
            Files.list(root.resolve("households/h1")).map {
                it.fileName.toString()
            }.toList(),
        )
        storage.delete("households/h1/abc.pdf")
        assertNull(storage.get("households/h1/abc.pdf"))
        storage.delete("households/h1/abc.pdf")
    }

    @Test
    fun refusesKeysOutsideTheRoot() = runTest {
        assertFailsWith<IllegalArgumentException> { storage.put("../escape.pdf", byteArrayOf(1)) }
    }

    @Test
    fun reportsAnUnwritableRootAsATypedFailure() = runTest {
        val file = Files.createTempFile("rasoi", "not-a-dir")
        val result = FileDocumentStorage(file).put("a/b.pdf", byteArrayOf(1))
        assertEquals(AppResult.Failure(AppError.Unexpected("storage_unavailable")), result)
    }
}
