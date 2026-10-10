package app.rasoi.server

import app.rasoi.server.persistence.DatabaseFactory
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlin.time.Clock

fun main() {
    val config = ServerConfig.fromEnvironment()
    val database = DatabaseFactory.connect(config.database)
    val clock = Clock.System
    val graph = AppGraph(config, database, AppGraph.verifierFor(config, clock), clock)
    embeddedServer(Netty, port = config.port) {
        if (config.authMode == AuthMode.DEV) log.warn("RASOI_AUTH_MODE=dev: development sign-in is enabled")
        monitor.subscribe(ApplicationStopped) { database.close() }
        rasoiModule(graph)
    }.start(wait = true)
}
