package app.rasoi.core.domain.kitchen

import app.rasoi.core.model.Freshness
import app.rasoi.core.model.PantryItem
import kotlinx.datetime.LocalDate

/** One dish idea: the pantry items it uses, by name, and a short method. */
data class DishSuggestion(val name: String, val uses: List<String>, val summary: String)

/** A request for a chat model: [system] sets the rules, [user] lists what is in the kitchen. */
data class DishPrompt(val system: String, val user: String)

/**
 * Dish ideas from what is in the kitchen (design-plan 6.4). The prompt carries item names only, never anything about
 * people, and every idea the model returns is checked against the stock, so a dish that needs something the flat does
 * not have never reaches the screen.
 */
object DishSuggestions {
    const val MAX_DISHES = 5

    /** Only the stock matters here, not how soon it expires. */
    private val ANY_DAY = LocalDate(1970, 1, 1)

    fun prompt(items: List<PantryItem>, today: LocalDate): DishPrompt {
        val stock = UseFirst.rank(items, today)
        val lines = stock.joinToString("\n") { entry ->
            val note = if (entry.freshness == Freshness.FRESH) "" else " (use soon)"
            "- ${entry.item.name}$note"
        }
        val system = "You suggest simple home-cooked Indian dishes. Answer with a JSON object " +
            "{\"dishes\":[{\"name\":string,\"uses\":[string],\"summary\":string}]} with at most $MAX_DISHES dishes. " +
            "Each entry in \"uses\" must be copied from the ingredient list you are given; use no other ingredients " +
            "except water, salt and common spices. Prefer ingredients marked (use soon). " +
            "Keep each summary to two sentences."
        return DishPrompt(system, "Ingredients in the kitchen:\n$lines")
    }

    /** Keeps named dishes whose every ingredient is in the kitchen, without duplicates, at most [MAX_DISHES]. */
    fun restrictToStock(dishes: List<DishSuggestion>, items: List<PantryItem>): List<DishSuggestion> {
        val stock = UseFirst.rank(items, ANY_DAY).map { words(it.item.name) }
        return dishes
            .filter { dish ->
                dish.name.isNotBlank() &&
                    dish.uses.isNotEmpty() &&
                    dish.uses.all { ingredient ->
                        words(ingredient).let {
                            it.isNotEmpty() &&
                                stock.any { name -> name.containsAll(it) }
                        }
                    }
            }
            .distinctBy { it.name.trim().lowercase() }
            .take(MAX_DISHES)
    }

    private fun words(text: String): Set<String> = text.lowercase().map {
        if (it.isLetterOrDigit()) it else ' '
    }.joinToString("").split(' ').filter { it.isNotEmpty() }.toSet()
}
