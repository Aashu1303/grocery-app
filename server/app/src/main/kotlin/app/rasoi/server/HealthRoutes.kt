package app.rasoi.server

import app.rasoi.core.contract.HealthDto
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.healthRoutes(config: ServerConfig) {
    get("/health") { call.respond(HealthDto(status = "ok", version = config.version)) }
}
