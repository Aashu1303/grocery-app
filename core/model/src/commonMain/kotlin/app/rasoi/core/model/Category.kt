package app.rasoi.core.model

/** What a purchased item is (design-plan 6.4). Food categories feed the pantry; the rest are only split. */
enum class Category(val isFood: Boolean) {
    PRODUCE(true),
    DAIRY(true),
    EGGS_MEAT(true),
    BAKERY(true),
    STAPLES(true),
    SNACKS(true),
    BEVERAGES(true),
    CONDIMENTS(true),
    FROZEN(true),
    HOUSEHOLD(false),
    PERSONAL_CARE(false),
    OTHER(false),
}
