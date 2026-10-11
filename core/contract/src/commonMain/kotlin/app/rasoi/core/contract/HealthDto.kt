package app.rasoi.core.contract

import kotlinx.serialization.Serializable

@Serializable
data class HealthDto(val status: String, val version: String)
