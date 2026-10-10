package app.rasoi.server.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import java.io.Closeable

/** An open connection pool with the schema migrated to the latest Flyway version. */
class RasoiDatabase internal constructor(val database: Database, private val dataSource: HikariDataSource) : Closeable {
    override fun close() = dataSource.close()
}

object DatabaseFactory {
    fun connect(config: DatabaseConfig): RasoiDatabase {
        val dataSource = HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = config.url
                username = config.user
                password = config.password
                maximumPoolSize = config.maxPoolSize
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_READ_COMMITTED"
            },
        )
        Flyway.configure().dataSource(dataSource).load().migrate()
        return RasoiDatabase(Database.connect(dataSource), dataSource)
    }
}
