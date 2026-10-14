package app.rasoi.server

import app.rasoi.server.parsing.fallback.OpenAiChatClient
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
    val storageDir: String = "data/documents",
    /** The LLM fallback for unknown invoice formats; off unless all three RASOI_LLM_* variables are set. */
    val llm: OpenAiChatClient.Config? = null,
    /** Path of a Google service-account key file; push notifications are off unless it is set. */
    val fcmServiceAccountFile: String? = null,
    /** Set when an external scheduler runs the sweeps through `POST /internal/sweeps`; the in-process timer is off. */
    val sweepToken: SweepToken? = null,
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
                storageDir = env["RASOI_STORAGE_DIR"]?.takeIf { it.isNotBlank() } ?: "data/documents",
                llm = llmConfig(env),
                fcmServiceAccountFile = env["FCM_SERVICE_ACCOUNT_FILE"]?.takeIf { it.isNotBlank() },
                sweepToken = env["RASOI_SWEEP_TOKEN"]?.takeIf { it.isNotBlank() }?.let(::SweepToken),
            )
        }

        private fun llmConfig(env: Map<String, String>): OpenAiChatClient.Config? {
            val values = LLM_VARIABLES.map { env[it]?.takeIf(String::isNotBlank) }
            require(values.all { it == null } || values.none { it == null }) {
                "Set all of ${LLM_VARIABLES.joinToString()} or none"
            }
            val (baseUrl, apiKey, model) = values
            return if (baseUrl == null ||
                apiKey == null ||
                model == null
            ) {
                null
            } else {
                OpenAiChatClient.Config(baseUrl, apiKey, model)
            }
        }

        private val LLM_VARIABLES = listOf("RASOI_LLM_BASE_URL", "RASOI_LLM_API_KEY", "RASOI_LLM_MODEL")

        private fun Map<String, String>.required(name: String): String =
            requireNotNull(this[name]?.takeIf { it.isNotBlank() }) { "$name must be set" }
    }
}
