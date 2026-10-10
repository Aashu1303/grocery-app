package app.rasoi.server

import app.rasoi.server.persistence.DatabaseConfig
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

enum class AuthMode { FIREBASE, DEV }

/** Runtime configuration, read from environment variables only; nothing secret is committed. */
data class ServerConfig(
    val port: Int,
    val version: String,
    val database: DatabaseConfig,
    val authMode: AuthMode,
    val firebaseProjectId: String?,
    val sessionTtl: Duration = 30.days,
) {
    companion object {
        private const val DEFAULT_PORT = 8080

        fun fromEnvironment(env: Map<String, String> = System.getenv()): ServerConfig {
            val authMode = env["RASOI_AUTH_MODE"]?.let { AuthMode.valueOf(it.uppercase()) } ?: AuthMode.FIREBASE
            val projectId = env["FIREBASE_PROJECT_ID"]
            require(authMode != AuthMode.FIREBASE || !projectId.isNullOrBlank()) {
                "FIREBASE_PROJECT_ID is required when RASOI_AUTH_MODE=firebase"
            }
            return ServerConfig(
                port = env["PORT"]?.toIntOrNull() ?: DEFAULT_PORT,
                version = env["RASOI_VERSION"] ?: "dev",
                database = DatabaseConfig(
                    url = env.required("DATABASE_URL"),
                    user = env.required("DATABASE_USER"),
                    password = env.required("DATABASE_PASSWORD"),
                ),
                authMode = authMode,
                firebaseProjectId = projectId,
                sessionTtl = env["SESSION_TTL_DAYS"]?.toLongOrNull()?.days ?: 30.days,
            )
        }

        private fun Map<String, String>.required(name: String): String =
            requireNotNull(this[name]?.takeIf { it.isNotBlank() }) { "$name must be set" }
    }
}
