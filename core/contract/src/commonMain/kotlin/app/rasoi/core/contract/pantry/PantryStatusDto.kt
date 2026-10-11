package app.rasoi.core.contract.pantry

import kotlinx.serialization.Serializable

@Serializable
enum class PantryStatusDto { IN_STOCK, FINISHED, DISCARDED }
