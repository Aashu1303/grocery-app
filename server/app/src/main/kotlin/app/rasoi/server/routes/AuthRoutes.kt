package app.rasoi.server.routes

import app.rasoi.core.contract.auth.CreateSessionRequestDto
import app.rasoi.core.contract.auth.SessionDto
import app.rasoi.core.model.map
import app.rasoi.server.http.respondResult
import app.rasoi.server.http.toDto
import app.rasoi.server.service.AuthService
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

/** POST /auth/session: exchange an ID token for a session. Naturally idempotent, so no key is required. */
fun Route.authRoutes(auth: AuthService) {
    post("/auth/session") {
        val request = call.receive<CreateSessionRequestDto>()
        call.respondResult(
            auth.signIn(request.idToken, request.displayName).map {
                it.toDto()
            },
            SessionDto.serializer(),
        )
    }
}
