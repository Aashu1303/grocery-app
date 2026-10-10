package app.rasoi.server

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

private const val DEFAULT_PORT = 8080

fun main() {
    val config = ServerConfig(
        port = System.getenv("PORT")?.toIntOrNull() ?: DEFAULT_PORT,
        version = System.getenv("RASOI_VERSION") ?: "dev",
    )
    embeddedServer(Netty, port = config.port) { rasoiModule(config) }.start(wait = true)
}
