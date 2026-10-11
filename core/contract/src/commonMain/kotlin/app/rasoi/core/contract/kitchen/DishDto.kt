package app.rasoi.core.contract.kitchen

import kotlinx.serialization.Serializable

/** A dish idea built only from items in the kitchen; [uses] names them. */
@Serializable
data class DishDto(val name: String, val uses: List<String>, val summary: String)
