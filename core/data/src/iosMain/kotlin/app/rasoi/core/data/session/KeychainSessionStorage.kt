package app.rasoi.core.data.session

import app.rasoi.core.contract.ContractJson
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import kotlinx.serialization.SerializationException

/** Stores the session in the iOS Keychain under the app's service name. */
@OptIn(ExperimentalSettingsImplementation::class)
class KeychainSessionStorage : SessionStorage {
    private val keychain = KeychainSettings(service = SERVICE)

    override suspend fun read(): StoredSession? {
        val json = keychain.getStringOrNull(KEY_SESSION) ?: return null
        return try {
            ContractJson.decodeFromString(StoredSession.serializer(), json)
        } catch (_: SerializationException) {
            keychain.remove(KEY_SESSION)
            null
        }
    }

    override suspend fun write(session: StoredSession) {
        keychain.putString(KEY_SESSION, ContractJson.encodeToString(StoredSession.serializer(), session))
    }

    override suspend fun clear() {
        keychain.remove(KEY_SESSION)
    }

    private companion object {
        const val SERVICE = "app.rasoi.session"
        const val KEY_SESSION = "session"
    }
}
