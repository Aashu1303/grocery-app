package app.rasoi.core.contract

import kotlinx.serialization.Serializable

/** Error body returned by every endpoint; [code] is stable and machine-readable, [message] is for logs. */
@Serializable
data class ErrorDto(val code: String, val message: String, val fields: List<FieldErrorDto> = emptyList())
