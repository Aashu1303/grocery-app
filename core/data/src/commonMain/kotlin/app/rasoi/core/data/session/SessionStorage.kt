package app.rasoi.core.data.session

/** Persists the session in the platform's secure store (Android Keystore, iOS Keychain). */
interface SessionStorage {
    suspend fun read(): StoredSession?

    suspend fun write(session: StoredSession)

    suspend fun clear()
}
