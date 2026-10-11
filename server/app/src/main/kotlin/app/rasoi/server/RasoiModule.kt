package app.rasoi.server

import app.rasoi.core.contract.ContractJson
import app.rasoi.core.contract.ErrorDto
import app.rasoi.core.model.AppError
import app.rasoi.server.auth.UserPrincipal
import app.rasoi.server.http.describeForLog
import app.rasoi.server.http.toHttp
import app.rasoi.server.routes.authRoutes
import app.rasoi.server.routes.billRoutes
import app.rasoi.server.routes.householdRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing

private const val SESSION_AUTH = "session"

fun Application.rasoiModule(graph: AppGraph) {
    install(ContentNegotiation) { json(ContractJson) }
    install(Authentication) {
        bearer(SESSION_AUTH) {
            authenticate { credential -> graph.auth.authenticate(credential.token)?.let(::UserPrincipal) }
        }
    }
    install(StatusPages) {
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(status, ErrorDto(code = "not_found", message = "No such resource"))
        }
        status(HttpStatusCode.Unauthorized) { call, _ ->
            val (status, body) = AppError.Unauthenticated.toHttp()
            call.respond(status, body)
        }
        exception<BadRequestException> { call, _ ->
            val (status, body) = AppError.Validation(listOf(AppError.FieldViolation("body", "malformed"))).toHttp()
            call.respond(status, body)
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled server error: {}", describeForLog(cause))
            call.respond(HttpStatusCode.InternalServerError, ErrorDto(code = "internal", message = "Unexpected error"))
        }
    }
    routing {
        healthRoutes(graph.config)
        authRoutes(graph.auth)
        authenticate(SESSION_AUTH) {
            householdRoutes(graph.households, graph.idempotency)
            billRoutes(graph.imports, graph.bills, graph.idempotency)
        }
    }
}
