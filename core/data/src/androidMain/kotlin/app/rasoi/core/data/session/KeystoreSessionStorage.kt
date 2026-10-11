package app.rasoi.core.data.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import app.rasoi.core.contract.ContractJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts the session with an AES-GCM key that never leaves the Android Keystore. */
class KeystoreSessionStorage(context: Context) : SessionStorage {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    override suspend fun read(): StoredSession? = withContext(Dispatchers.IO) {
        val encoded = preferences.getString(KEY_SESSION, null) ?: return@withContext null
        try {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
            val json = cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES).decodeToString()
            ContractJson.decodeFromString(StoredSession.serializer(), json)
        } catch (_: GeneralSecurityException) {
            preferences.edit().remove(KEY_SESSION).apply()
            null
        } catch (_: IllegalArgumentException) {
            preferences.edit().remove(KEY_SESSION).apply()
            null
        }
    }

    override suspend fun write(session: StoredSession) = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val plain = ContractJson.encodeToString(StoredSession.serializer(), session).encodeToByteArray()
        val encrypted = cipher.doFinal(plain)
        val payload = cipher.iv + encrypted
        preferences.edit().putString(KEY_SESSION, Base64.encodeToString(payload, Base64.NO_WRAP)).apply()
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        preferences.edit().remove(KEY_SESSION).apply()
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
        const val PREFERENCES = "rasoi_session"
        const val KEY_SESSION = "session"
        const val KEY_ALIAS = "rasoi_session_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        const val KEY_BITS = 256
    }
}
