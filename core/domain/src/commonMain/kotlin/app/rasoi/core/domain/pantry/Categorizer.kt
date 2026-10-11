package app.rasoi.core.domain.pantry

import app.rasoi.core.model.Category
import app.rasoi.core.model.Platform

/**
 * Design-plan 6.4 item 6: keyword rules on the item name come first (so "milk" is dairy whatever the HSN says),
 * then the HSN chapter, then a hint from the platform (Handpickd sells only produce). Anything else is not food
 * until the user says so.
 */
object Categorizer {
    private class Rule(val category: Category, val keywords: List<String>)

    /** Order matters: processed foods come before the raw ingredient they contain ("potato chips"). */
    private val rules = listOf(
        Rule(
            Category.PERSONAL_CARE,
            listOf(
                "shampoo",
                "conditioner",
                "toothpaste",
                "toothbrush",
                "deodorant",
                "lotion",
                "face wash",
                "razor",
            ),
        ),
        Rule(
            Category.HOUSEHOLD,
            listOf("detergent", "dishwash", "cleaner", "tissue", "foil", "garbage bag", "battery", "bulb", "mop"),
        ),
        Rule(Category.FROZEN, listOf("ice cream", "frozen", "kulfi")),
        Rule(
            Category.SNACKS,
            listOf(
                "chip", "namkeen", "bhujia", "biscuit", "cookie", "mathri", "papdi", "papad", "sev", "snack",
                "chocolate", "wafer", "popcorn", "noodle", "maggi", "instant",
            ),
        ),
        Rule(Category.BAKERY, listOf("bread", "bun", "pav", "cake", "rusk", "croissant", "bagel")),
        Rule(
            Category.CONDIMENTS,
            listOf("ketchup", "sauce", "chutney", "pickle", "jam", "mayonnaise", "peanut butter", "vinegar", "dip"),
        ),
        Rule(
            Category.BEVERAGES,
            listOf(
                "cola", "juice", "drink", "soda", "tea", "coffee", "water", "squash", "energy", "lassi", "buttermilk",
            ),
        ),
        Rule(
            Category.DAIRY,
            listOf("milk", "curd", "dahi", "paneer", "cheese", "butter", "yogurt", "yoghurt", "cream"),
        ),
        Rule(
            Category.EGGS_MEAT,
            listOf("egg", "chicken", "mutton", "fish", "prawn", "lamb", "pork", "salmon", "meat"),
        ),
        Rule(
            Category.STAPLES,
            listOf(
                "rice", "atta", "flour", "maida", "besan", "sooji", "rava", "poha", "oats", "dal", "lentil", "rajma",
                "chana", "oil", "ghee", "sugar", "jaggery", "salt", "masala", "spice", "turmeric", "cumin", "pasta",
            ),
        ),
        Rule(
            Category.PRODUCE,
            listOf(
                "potato", "onion", "tomato", "garlic", "ginger", "chilli", "lemon", "banana", "apple", "mango",
                "orange", "grape", "melon", "mellon", "papaya", "gourd", "beans", "pumpkin", "brinjal", "okra",
                "bhindi", "spinach", "palak", "methi", "coriander", "mint", "cabbage", "cauliflower", "carrot",
                "cucumber", "peas", "capsicum", "radish", "beetroot", "colocasia", "arbi", "vegetable", "fruit",
            ),
        ),
    )

    fun categorize(name: String, hsn: String?, platform: Platform): Categorization {
        val category =
            byKeyword(name) ?: hsn?.let(::byHsn) ?: Category.PRODUCE.takeIf { platform == Platform.HANDPICKD }
        return Categorization(category ?: Category.OTHER)
    }

    private fun byKeyword(name: String): Category? {
        val words = " " + name.lowercase().map { if (it.isLetter()) it else ' ' }.joinToString("") + " "
        return rules.firstOrNull { rule -> rule.keywords.any { matches(words, it) } }?.category
    }

    private fun matches(words: String, keyword: String): Boolean =
        words.contains(" $keyword ") || words.contains(" ${keyword}s ") || words.contains(" ${keyword}es ")

    /** Headings that differ from the rest of their chapter win over [chapters]. */
    private val headings = mapOf(
        "0407" to Category.EGGS_MEAT,
        "0408" to Category.EGGS_MEAT,
        "1905" to Category.SNACKS,
        "2103" to Category.CONDIMENTS,
        "2105" to Category.FROZEN,
    )

    private val chapters = mapOf(
        "02" to Category.EGGS_MEAT,
        "03" to Category.EGGS_MEAT,
        "16" to Category.EGGS_MEAT,
        "04" to Category.DAIRY,
        "07" to Category.PRODUCE,
        "08" to Category.PRODUCE,
        "09" to Category.STAPLES,
        "10" to Category.STAPLES,
        "11" to Category.STAPLES,
        "12" to Category.STAPLES,
        "15" to Category.STAPLES,
        "17" to Category.STAPLES,
        "19" to Category.STAPLES,
        "25" to Category.STAPLES,
        "18" to Category.SNACKS,
        "21" to Category.SNACKS,
        "20" to Category.CONDIMENTS,
        "22" to Category.BEVERAGES,
        "33" to Category.PERSONAL_CARE,
        "34" to Category.HOUSEHOLD,
        "39" to Category.HOUSEHOLD,
        "48" to Category.HOUSEHOLD,
        "49" to Category.HOUSEHOLD,
        "73" to Category.HOUSEHOLD,
    )

    private fun byHsn(hsn: String): Category? {
        val digits = hsn.filter(Char::isDigit)
        return headings[digits.take(HEADING_DIGITS)] ?: chapters[digits.take(CHAPTER_DIGITS)]
    }

    private const val CHAPTER_DIGITS = 2
    private const val HEADING_DIGITS = 4
}
