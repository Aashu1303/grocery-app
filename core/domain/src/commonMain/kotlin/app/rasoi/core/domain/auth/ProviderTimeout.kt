package app.rasoi.core.domain.auth

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * How long a sign-in step may wait for the identity provider. Native SDKs answer through callbacks that can never
 * arrive (an app check that does not finish, for one), which would leave sign-in spinning forever. Matches the HTTP
 * request timeout.
 */
internal val PROVIDER_TIMEOUT = 30.seconds

internal suspend fun <T> withProviderTimeout(call: suspend () -> AppResult<T>): AppResult<T> =
    withTimeoutOrNull(PROVIDER_TIMEOUT) { call() } ?: AppResult.Failure(AppError.Timeout)
