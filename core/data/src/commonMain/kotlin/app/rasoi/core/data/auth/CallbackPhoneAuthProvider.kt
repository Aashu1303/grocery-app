package app.rasoi.core.data.auth

import app.rasoi.core.domain.auth.PhoneAuthProvider
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.PhoneNumber
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Adapts a native sign-in SDK that reports through callbacks, as the Firebase iOS SDK does from Swift (ADR 0024).
 * Each callback gets `(value, errorCode)`: a verification id or ID token on success, otherwise a code, where
 * [INVALID] means the user typed a bad number or code and [OFFLINE] means no connection.
 */
class CallbackPhoneAuthProvider(
    private val requestCode: (phone: String, done: (String?, String?) -> Unit) -> Unit,
    private val confirmCode: (verificationId: String, code: String, done: (String?, String?) -> Unit) -> Unit,
) : PhoneAuthProvider {
    override suspend fun requestCode(phone: PhoneNumber): AppResult<String> =
        await("phone") { done -> requestCode(phone.e164, done) }

    override suspend fun confirm(verificationId: String, code: String): AppResult<String> =
        await("code") { done -> confirmCode(verificationId, code, done) }

    private suspend fun await(field: String, start: (done: (String?, String?) -> Unit) -> Unit): AppResult<String> =
        suspendCancellableCoroutine { continuation ->
            start { value, error ->
                if (continuation.isActive) continuation.resume(result(field, value, error))
            }
        }

    private fun result(field: String, value: String?, error: String?): AppResult<String> = when {
        error == INVALID -> AppResult.Failure(
            AppError.Validation(
                listOf(
                    AppError.FieldViolation(
                        field,
                        if (field ==
                            "code"
                        ) {
                            "incorrect"
                        } else {
                            "invalid"
                        },
                    ),
                ),
            ),
        )
        error == OFFLINE -> AppResult.Failure(AppError.Offline)
        error != null -> AppResult.Failure(AppError.Unexpected(error))
        value != null -> AppResult.Success(value)
        else -> AppResult.Failure(AppError.Unexpected("empty_result"))
    }

    companion object {
        const val INVALID = "invalid"
        const val OFFLINE = "offline"
    }
}
