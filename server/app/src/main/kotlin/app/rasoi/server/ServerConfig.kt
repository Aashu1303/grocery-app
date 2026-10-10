package app.rasoi.server

/** Runtime configuration, read from environment variables only. */
data class ServerConfig(val port: Int, val version: String)
