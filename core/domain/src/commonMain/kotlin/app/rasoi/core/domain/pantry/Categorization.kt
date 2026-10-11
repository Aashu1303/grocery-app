package app.rasoi.core.domain.pantry

import app.rasoi.core.model.Category

/** The category of a bill line and whether it belongs in the pantry. */
data class Categorization(val category: Category, val isFood: Boolean = category.isFood)
