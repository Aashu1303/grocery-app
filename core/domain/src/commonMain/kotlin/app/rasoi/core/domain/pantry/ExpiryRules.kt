package app.rasoi.core.domain.pantry

import app.rasoi.core.model.Category
import app.rasoi.core.model.Freshness
import app.rasoi.core.model.LocationKind
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

/**
 * Design-plan 6.4 item 7: a default shelf life per category with a few name-specific overrides. These are
 * estimates for the "use first" list, editable per item, never a food-safety statement.
 */
object ExpiryRules {
    private const val USE_SOON_DAYS = 2

    private class Override(val keywords: List<String>, val days: Int, val categories: Set<Category>)

    private val overrides = listOf(
        Override(listOf("milk"), days = 2, categories = setOf(Category.DAIRY)),
        Override(listOf("curd", "dahi", "yogurt", "yoghurt"), days = 6, categories = setOf(Category.DAIRY)),
        Override(
            listOf("palak", "spinach", "methi", "coriander", "mint", "leaf", "leafy"),
            days = 3,
            categories = setOf(Category.PRODUCE),
        ),
        Override(listOf("potato", "onion", "garlic"), days = 21, categories = setOf(Category.PRODUCE)),
        Override(listOf("egg"), days = 21, categories = setOf(Category.EGGS_MEAT)),
        Override(
            listOf("chicken", "mutton", "fish", "prawn", "lamb", "pork", "meat"),
            days = 2,
            categories = setOf(Category.EGGS_MEAT),
        ),
    )

    private val categoryDays = mapOf(
        Category.PRODUCE to 5,
        Category.DAIRY to 5,
        Category.EGGS_MEAT to 2,
        Category.BAKERY to 3,
        Category.STAPLES to 180,
        Category.SNACKS to 90,
        Category.BEVERAGES to 180,
        Category.CONDIMENTS to 180,
        Category.FROZEN to 90,
    )

    /** The estimated last good day for food bought on [bought]; null for non-food. */
    fun estimate(name: String, category: Category, bought: LocalDate): LocalDate? {
        val days = overrideDays(name, category) ?: categoryDays[category] ?: return null
        return bought.plus(DatePeriod(days = days))
    }

    /** Where this item is usually kept: fridge for dairy, eggs, meat and most produce; freezer for frozen. */
    fun defaultLocation(name: String, category: Category): LocationKind = when {
        category == Category.FROZEN -> LocationKind.FREEZER
        category == Category.PRODUCE && words(name).any { it in SHELF_PRODUCE } -> LocationKind.SHELF
        category in FRIDGE_CATEGORIES -> LocationKind.FRIDGE
        else -> LocationKind.SHELF
    }

    fun daysLeft(expiresOn: LocalDate, today: LocalDate): Int = today.daysUntil(expiresOn)

    fun freshness(expiresOn: LocalDate, today: LocalDate): Freshness = when {
        daysLeft(expiresOn, today) < 0 -> Freshness.EXPIRED
        daysLeft(expiresOn, today) <= USE_SOON_DAYS -> Freshness.USE_SOON
        else -> Freshness.FRESH
    }

    private fun overrideDays(name: String, category: Category): Int? {
        val words = words(name)
        return overrides.firstOrNull { rule -> category in rule.categories && rule.keywords.any { it in words } }?.days
    }

    private fun words(name: String): Set<String> =
        name.lowercase().split(Regex("[^a-z]+")).filter { it.isNotEmpty() }.map { it.removeSuffix("s") }.toSet()

    private val SHELF_PRODUCE = setOf("potato", "onion", "garlic", "ginger", "banana")
    private val FRIDGE_CATEGORIES = setOf(Category.PRODUCE, Category.DAIRY, Category.EGGS_MEAT)
}
