package app.rasoi.server.notifications

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

/** The parts of a Google service-account key file that signing a push request needs. */
class ServiceAccount(val projectId: String, val clientEmail: String, val privateKey: RSAPrivateKey) {
    override fun toString() = "ServiceAccount(projectId=$projectId, clientEmail=$clientEmail)"

    companion object {
        private val BEGIN = Regex("-----BEGIN [A-Z ]+-----")
        private val END = Regex("-----END [A-Z ]+-----")

        /** Null when [json] is not a usable service-account file; the key is never logged. */
        fun parse(json: String): ServiceAccount? = try {
            val root = Json.parseToJsonElement(json).jsonObject
            fun field(name: String) = root[name]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            val pem = field("private_key")
            val projectId = field("project_id")
            val email = field("client_email")
            if (pem == null || projectId == null || email == null) {
                null
            } else {
                val body = pem.replace(BEGIN, "").replace(END, "").filterNot(Char::isWhitespace)
                val key = KeyFactory.getInstance(
                    "RSA",
                ).generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(body)))
                ServiceAccount(projectId, email, key as RSAPrivateKey)
            }
        } catch (_: IllegalArgumentException) { // malformed JSON or Base64
            null
        } catch (_: java.security.GeneralSecurityException) {
            null
        }
    }
}
