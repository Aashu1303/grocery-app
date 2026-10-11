package app.rasoi.server.http

import app.rasoi.core.contract.ApiHeaders
import app.rasoi.core.contract.ContractJson
import app.rasoi.core.contract.ErrorDto
import app.rasoi.core.model.AppError
import app.rasoi.core.model.UserId
import app.rasoi.server.persistence.IdempotencyStore
import app.rasoi.server.persistence.StoredResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receiveChannel
import io.ktor.utils.io.readBuffer
import kotlinx.io.readByteArray
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import java.security.MessageDigest
import kotlin.time.Clock

/**
 * Executes a mutating request at most once per (user, `Idempotency-Key`). A retry with the same key and body
 * replays the stored response; the same key with a different body is rejected (design-plan 3, correctness 7).
 */
internal class Idempotency(private val store: IdempotencyStore, private val clock: Clock) {
    suspend fun <Req> handle(
        call: ApplicationCall,
        userId: UserId,
        requestSerializer: KSerializer<Req>,
        maxBodyBytes: Long = DEFAULT_MAX_BODY,
        block: suspend (Req) -> JsonResponse,
    ) {
        val key = call.request.headers[ApiHeaders.IDEMPOTENCY_KEY]?.takeIf { it.length in KEY_LENGTH }
        val body = call.receiveChannel().readBuffer(maxBodyBytes + 1).readByteArray()
            .takeIf { it.size <= maxBodyBytes }
            ?.decodeToString()
            ?: return call.respondJson(
                AppError.Validation(listOf(AppError.FieldViolation("body", "too_large"))).asResponse(),
            )
        val requestHash = sha256("${call.request.httpMethod.value} ${call.request.path()}\n$body")
        val stored = key?.let { store.find(userId, it) }
        val request = if (key == null || stored != null) null else decode(requestSerializer, body)
        val response = when {
            key == null -> missingKey().asResponse()
            stored != null && stored.requestHash == requestHash ->
                JsonResponse(HttpStatusCode.fromValue(stored.statusCode), stored.body)
            stored != null -> AppError.Conflict("idempotency_key_reused").asResponse()
            request == null -> AppError.Validation(listOf(AppError.FieldViolation("body", "malformed"))).asResponse()
            else -> block(request.value).also { fresh ->
                if (fresh.status.value < SERVER_ERROR) {
                    store.save(userId, key, StoredResponse(requestHash, fresh.status.value, fresh.body), clock.now())
                }
            }
        }
        call.respondJson(response)
    }

    private class Decoded<T>(val value: T)

    private fun <Req> decode(serializer: KSerializer<Req>, body: String): Decoded<Req>? = try {
        Decoded(ContractJson.decodeFromString(serializer, body))
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun AppError.asResponse(): JsonResponse = toHttp().let { (status, dto) ->
        JsonResponse(status, ContractJson.encodeToString(ErrorDto.serializer(), dto))
    }

    private fun missingKey() =
        AppError.Validation(listOf(AppError.FieldViolation(ApiHeaders.IDEMPOTENCY_KEY, "required")))

    private fun sha256(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private companion object {
        val KEY_LENGTH = 8..128
        const val SERVER_ERROR = 500
        const val DEFAULT_MAX_BODY = 64L * 1024
    }
}
