package app.rasoi.core.data.network

import app.rasoi.core.contract.ApiHeaders
import app.rasoi.core.contract.ContractJson
import app.rasoi.core.contract.ErrorDto
import app.rasoi.core.contract.auth.CreateSessionRequestDto
import app.rasoi.core.contract.auth.MeDto
import app.rasoi.core.contract.auth.SessionDto
import app.rasoi.core.contract.bills.BillDto
import app.rasoi.core.contract.bills.UpdateBillRequestDto
import app.rasoi.core.contract.bills.UploadInvoicesRequestDto
import app.rasoi.core.contract.household.CreateHouseholdRequestDto
import app.rasoi.core.contract.household.CreateInviteRequestDto
import app.rasoi.core.contract.household.CreateLocationRequestDto
import app.rasoi.core.contract.household.HouseholdDto
import app.rasoi.core.contract.household.InviteDto
import app.rasoi.core.contract.household.LocationDto
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlin.uuid.Uuid

/**
 * Typed client for the Rasoi server. Every call returns an [AppResult]; network failures, HTTP errors and
 * malformed bodies become [AppError]s. Writes carry one idempotency key that is reused on retries.
 */
class RasoiApi(
    private val client: HttpClient,
    private val config: ApiConfig,
    private val token: () -> String?,
    private val onUnauthenticated: suspend () -> Unit,
    private val newIdempotencyKey: () -> String = { Uuid.random().toString() },
) {
    suspend fun createSession(request: CreateSessionRequestDto): AppResult<SessionDto> =
        send(HttpMethod.Post, "/auth/session", SessionDto.serializer(), authenticated = false) {
            jsonBody(request)
        }

    suspend fun me(): AppResult<MeDto> = send(HttpMethod.Get, "/me", MeDto.serializer())

    suspend fun createHousehold(request: CreateHouseholdRequestDto): AppResult<HouseholdDto> =
        write("/households", HouseholdDto.serializer()) { jsonBody(request) }

    suspend fun household(id: String): AppResult<HouseholdDto> =
        send(HttpMethod.Get, "/households/$id", HouseholdDto.serializer())

    suspend fun createInvite(householdId: String, request: CreateInviteRequestDto): AppResult<InviteDto> =
        write("/households/$householdId/invites", InviteDto.serializer()) { jsonBody(request) }

    suspend fun acceptInvite(code: String): AppResult<HouseholdDto> =
        write("/invites/$code/accept", HouseholdDto.serializer()) { jsonBody(emptyMap<String, String>()) }

    suspend fun addLocation(householdId: String, request: CreateLocationRequestDto): AppResult<LocationDto> =
        write("/households/$householdId/locations", LocationDto.serializer()) { jsonBody(request) }

    suspend fun importInvoices(householdId: String, request: UploadInvoicesRequestDto): AppResult<BillDto> =
        write("/households/$householdId/invoices", BillDto.serializer()) { jsonBody(request) }

    suspend fun bill(id: String): AppResult<BillDto> = send(HttpMethod.Get, "/bills/$id", BillDto.serializer())

    suspend fun updateBill(id: String, request: UpdateBillRequestDto): AppResult<BillDto> =
        write("/bills/$id", BillDto.serializer(), HttpMethod.Patch) { jsonBody(request) }

    private suspend fun <T> write(
        path: String,
        serializer: KSerializer<T>,
        method: HttpMethod = HttpMethod.Post,
        body: HttpRequestBuilder.() -> Unit,
    ): AppResult<T> {
        val key = newIdempotencyKey()
        var attempt = 0
        while (true) {
            val result = send(method, path, serializer) {
                header(ApiHeaders.IDEMPOTENCY_KEY, key)
                body()
            }
            val retryable = result is AppResult.Failure &&
                (result.error == AppError.Offline || result.error == AppError.Timeout)
            if (!retryable || ++attempt > MAX_WRITE_RETRIES) return result
        }
    }

    private suspend fun <T> send(
        method: HttpMethod,
        path: String,
        serializer: KSerializer<T>,
        authenticated: Boolean = true,
        configure: HttpRequestBuilder.() -> Unit = {},
    ): AppResult<T> = try {
        val response = client.request(config.baseUrl.trimEnd('/') + path) {
            this.method = method
            if (authenticated) token()?.let(::bearerAuth)
            configure()
        }
        val text = response.bodyAsText()
        if (response.status.isSuccess()) {
            AppResult.Success(ContractJson.decodeFromString(serializer, text))
        } else {
            val error = errorFor(response.status, text)
            if (error == AppError.Unauthenticated && authenticated) onUnauthenticated()
            AppResult.Failure(error)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: HttpRequestTimeoutException) {
        AppResult.Failure(AppError.Timeout)
    } catch (_: IOException) {
        AppResult.Failure(AppError.Offline)
    } catch (_: SerializationException) {
        AppResult.Failure(AppError.Unexpected("malformed response"))
    } catch (_: IllegalArgumentException) {
        AppResult.Failure(AppError.Unexpected("malformed response"))
    }

    private fun errorFor(status: HttpStatusCode, body: String): AppError {
        val dto = try {
            ContractJson.decodeFromString(ErrorDto.serializer(), body)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
        return when (status) {
            HttpStatusCode.BadRequest ->
                AppError.Validation(dto?.fields.orEmpty().map { AppError.FieldViolation(it.field, it.code) })
            HttpStatusCode.Unauthorized -> AppError.Unauthenticated
            HttpStatusCode.Forbidden -> AppError.Forbidden
            HttpStatusCode.NotFound -> AppError.NotFound(dto?.message?.removePrefix("No such ") ?: "resource")
            HttpStatusCode.Conflict -> AppError.Conflict(dto?.code ?: "conflict")
            else -> AppError.Server(status.value)
        }
    }

    private inline fun <reified B> HttpRequestBuilder.jsonBody(body: B) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private companion object {
        const val MAX_WRITE_RETRIES = 2
    }
}
