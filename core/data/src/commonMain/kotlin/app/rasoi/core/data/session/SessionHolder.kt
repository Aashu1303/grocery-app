package app.rasoi.core.data.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

/** The single in-memory source of the current session, backed by [SessionStorage]. Expired sessions are dropped. */
class SessionHolder(private val storage: SessionStorage, private val clock: Clock) {
    private val mutex = Mutex()
    private val current = MutableStateFlow<StoredSession?>(null)
    val session: StateFlow<StoredSession?> = current.asStateFlow()

    suspend fun restore(): StoredSession? = mutex.withLock {
        val stored = storage.read()
        val valid = stored?.takeIf { it.expiresAt > clock.now() }
        if (stored != null && valid == null) storage.clear()
        current.value = valid
        valid
    }

    suspend fun save(session: StoredSession) = mutex.withLock {
        storage.write(session)
        current.value = session
    }

    suspend fun clear() = mutex.withLock {
        storage.clear()
        current.value = null
    }

    fun token(): String? = current.value?.takeIf { it.expiresAt > clock.now() }?.token
}
