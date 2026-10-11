package app.rasoi.core.data.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.rasoi.core.contract.ContractJson
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.dataStore by preferencesDataStore(name = "rasoi_session")

/** Encrypts the session with an AES-GCM key that never leaves the Android Keystore, backed by DataStore. */
class KeystoreSessionStorage(private val context: Context) : SessionStorage {

    override suspend fun read(): StoredSession? {
        val encoded = context.dataStore.data.first()[KEY_SESSION] ?: return null
        return try {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
            val json = cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES).decodeToString()
            ContractJson.decodeFromString(StoredSession.serializer(), json)
        } catch (_: GeneralSecurityException) {
            clear()
            null
        } catch (_: IllegalArgumentException) {
            clear()
            null
        } catch (_: SerializationException) {
            clear()
            null
        } catch (_: IOException) {
            clear()
            null
        }
    }

    override suspend fun write(session: StoredSession) {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
            val plain = ContractJson.encodeToString(StoredSession.serializer(), session).encodeToByteArray()
            val payload = cipher.iv + cipher.doFinal(plain)
            context.dataStore.edit { it[KEY_SESSION] = Base64.encodeToString(payload, Base64.NO_WRAP) }
        } catch (_: GeneralSecurityException) {
            // Keystore unavailable — session survives in memory only for this launch.
        } catch (_: IOException) {
            // DataStore write failed — same as above.
        }
    }

    override suspend fun clear() {
        context.dataStore.edit { it.remove(KEY_SESSION) }
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val purposes = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        val spec = KeyGenParameterSpec.Builder(KEY_ALIAS, purposes)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_BITS)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(spec)
            generateKey()
        }
    }

    private companion object {
        val KEY_SESSION = stringPreferencesKey("session")
        const val KEY_ALIAS = "rasoi_session_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        const val KEY_BITS = 256
    }
}
