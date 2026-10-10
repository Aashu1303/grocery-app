package app.rasoi.server

import app.rasoi.core.contract.ContractJson
import app.rasoi.core.contract.ErrorDto
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing

fun Application.rasoiModule(config: ServerConfig) {
    install(ContentNegotiation) { json(ContractJson) }
    install(StatusPages) {
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(status, ErrorDto(code = "not_found", message = "No such resource"))
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled server error", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorDto(code = "internal", message = "Unexpected error"))
        }
    }
    routing { healthRoutes(config) }
}
