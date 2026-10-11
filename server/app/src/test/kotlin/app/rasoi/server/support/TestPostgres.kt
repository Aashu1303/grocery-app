package app.rasoi.server.support

import app.rasoi.server.persistence.DatabaseConfig
import app.rasoi.server.persistence.DatabaseFactory
import app.rasoi.server.persistence.RasoiDatabase
import org.testcontainers.postgresql.PostgreSQLContainer

/** One PostgreSQL container per test JVM, migrated by Flyway; [reset] empties every table between tests. */
object TestPostgres {
    private val container by lazy {
        PostgreSQLContainer("postgres:17-alpine").apply { start() }
    }

    val database: RasoiDatabase by lazy {
        DatabaseFactory.connect(DatabaseConfig(container.jdbcUrl, container.username, container.password))
    }

    fun reset() {
        database
        container.createConnection("").use { connection ->
            connection.createStatement().use {
                it.execute(
                    "TRUNCATE users, households, memberships, invites, locations, sessions, idempotency_keys CASCADE",
                )
            }
        }
    }
}
