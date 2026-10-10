package app.rasoi.server.http

import io.ktor.http.HttpStatusCode

/** A response rendered to JSON text, so it can be stored and replayed byte for byte. */
internal data class JsonResponse(val status: HttpStatusCode, val body: String)
