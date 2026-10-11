package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

@Serializable
enum class CategoryDto {
    PRODUCE,
    DAIRY,
    EGGS_MEAT,
    BAKERY,
    STAPLES,
    SNACKS,
    BEVERAGES,
    CONDIMENTS,
    FROZEN,
    HOUSEHOLD,
    PERSONAL_CARE,
    OTHER,
}
