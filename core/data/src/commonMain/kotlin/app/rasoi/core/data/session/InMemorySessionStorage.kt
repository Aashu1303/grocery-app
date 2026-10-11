package app.rasoi.core.data.session

/** Non-persistent storage for desktop runs and tests. */
class InMemorySessionStorage(private var stored: StoredSession? = null) : SessionStorage {
    override suspend fun read(): StoredSession? = stored

    override suspend fun write(session: StoredSession) {
        stored = session
    }

    override suspend fun clear() {
        stored = null
    }
}
