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
}
