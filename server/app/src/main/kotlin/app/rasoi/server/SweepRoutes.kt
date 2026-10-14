package app.rasoi.server

import app.rasoi.core.contract.ErrorDto
import app.rasoi.core.model.AppError
import app.rasoi.server.http.describeForLog
import app.rasoi.server.http.toHttp
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.post
import kotlinx.coroutines.CancellationException

/**
 * Runs every sweep once, in order. A failing sweep does not stop the others; any failure answers 500 so the
 * scheduler records it and retries, which is safe because sweeps only do what is due.
 */
fun Route.sweepRoutes(token: SweepToken, graph: AppGraph) {
    post("/internal/sweeps") {
        if (!token.matches(call.request.headers[SweepToken.HEADER])) {
            val (status, body) = AppError.Unauthenticated.toHttp()
            return@post call.respond(status, body)
        }
        val failed = sweeps(graph).filter { (name, job) -> !runReporting(name) { job.run() } }.keys
        if (failed.isEmpty()) {
            call.respond(HttpStatusCode.NoContent)
        } else {
            call.respond(HttpStatusCode.InternalServerError, ErrorDto(code = "internal", message = "Sweeps failed"))
        }
    }
}

// A sweep must never fail the others, whatever it throws; cancellation still propagates.
@Suppress("TooGenericExceptionCaught")
private suspend fun Route.runReporting(name: String, sweep: suspend () -> Unit): Boolean = try {
    sweep()
    true
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    application.log.error("Sweep {} failed: {}", name, describeForLog(error))
    false
}
