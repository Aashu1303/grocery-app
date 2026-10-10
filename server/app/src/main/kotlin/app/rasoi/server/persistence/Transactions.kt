package app.rasoi.server.persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

/** Runs [block] in a database transaction on the IO dispatcher. */
internal suspend fun <T> RasoiDatabase.query(block: suspend JdbcTransaction.() -> T): T =
    withContext(Dispatchers.IO) { suspendTransaction(database) { block() } }
