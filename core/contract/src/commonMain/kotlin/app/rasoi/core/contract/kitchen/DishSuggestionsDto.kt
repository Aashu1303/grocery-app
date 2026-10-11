package app.rasoi.core.contract.kitchen

import kotlinx.serialization.Serializable

@Serializable
data class DishSuggestionsDto(val dishes: List<DishDto>)
