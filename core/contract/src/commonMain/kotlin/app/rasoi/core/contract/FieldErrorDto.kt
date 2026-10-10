package app.rasoi.core.contract

import kotlinx.serialization.Serializable

@Serializable
data class FieldErrorDto(val field: String, val code: String)
