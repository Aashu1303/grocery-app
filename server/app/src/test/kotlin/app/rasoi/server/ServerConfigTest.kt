package app.rasoi.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.days

class ServerConfigTest {
    private val database = mapOf(
        "DATABASE_URL" to "jdbc:postgresql://localhost/rasoi",
        "DATABASE_USER" to "rasoi",
        "DATABASE_PASSWORD" to "secret",
    )

    @Test
    fun readsEnvironmentWithDefaults() {
        val config = ServerConfig.fromEnvironment(database + ("FIREBASE_PROJECT_ID" to "rasoi"))
        assertEquals(8080, config.port)
        assertEquals(AuthMode.FIREBASE, config.authMode)
        assertEquals(30.days, config.sessionTtl)
    }

    @Test
    fun firebaseModeNeedsAProjectId() {
        assertFailsWith<IllegalArgumentException> { ServerConfig.fromEnvironment(database) }
        assertEquals(AuthMode.DEV, ServerConfig.fromEnvironment(database + ("RASOI_AUTH_MODE" to "dev")).authMode)
    }

    @Test
    fun databaseSettingsAreRequiredAndPasswordIsNeverPrinted() {
        assertFailsWith<IllegalArgumentException> { ServerConfig.fromEnvironment(mapOf("RASOI_AUTH_MODE" to "dev")) }
        val config = ServerConfig.fromEnvironment(database + ("RASOI_AUTH_MODE" to "dev"))
        assertFalse(config.toString().contains("secret"))
    }

    @Test
    fun storageDefaultsAndTheLlmFallbackNeedsAllThreeSettings() {
        val dev = database + ("RASOI_AUTH_MODE" to "dev")
        val plain = ServerConfig.fromEnvironment(dev)
        assertEquals("data/documents", plain.storageDir)
        assertEquals(null, plain.llm)
        val llm = mapOf(
            "RASOI_LLM_BASE_URL" to "https://llm.example/v1",
            "RASOI_LLM_API_KEY" to "llm-secret",
            "RASOI_LLM_MODEL" to "small",
        )
        val configured = ServerConfig.fromEnvironment(dev + llm + ("RASOI_STORAGE_DIR" to "/srv/rasoi"))
        assertEquals("/srv/rasoi", configured.storageDir)
        assertEquals("small", configured.llm?.model)
        assertFalse(configured.toString().contains("llm-secret"))
        assertFailsWith<IllegalArgumentException> { ServerConfig.fromEnvironment(dev + llm.minus("RASOI_LLM_MODEL")) }
    }
}
