package app.rasoi.core.domain.kitchen

import app.rasoi.core.model.Category
import app.rasoi.core.model.PantryStatus
import app.rasoi.core.model.Quantity
import app.rasoi.core.testing.Builders
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DishSuggestionsTest {
    private val today = LocalDate(2026, 10, 11)
    private val items = listOf(
        Builders.pantryItem("a", "Tomato Hybrid", Category.PRODUCE, Quantity.grams(500), "l1", LocalDate(2026, 10, 12)),
        Builders.pantryItem("b", "Amul Paneer", Category.DAIRY, Quantity.grams(200), "l1", LocalDate(2026, 10, 14)),
        Builders.pantryItem("c", "Basmati Rice", Category.STAPLES, Quantity.kilograms(1), "l2", null),
        Builders.pantryItem(
            "d",
            "Old Spinach",
            Category.PRODUCE,
            Quantity.grams(100),
            "l1",
            null,
            PantryStatus.DISCARDED,
        ),
        Builders.pantryItem("e", "Dishwash", Category.HOUSEHOLD, Quantity.count(1), "l2", null),
    )

    @Test
    fun thePromptNamesOnlyFoodInStockWithTheMostUrgentFirst() {
        val prompt = DishSuggestions.prompt(items, today)
        val lines = prompt.user.lines().filter { it.startsWith("- ") }
        assertEquals(listOf("- Tomato Hybrid (use soon)", "- Amul Paneer", "- Basmati Rice"), lines)
        assertFalse("Spinach" in prompt.user || "Dishwash" in prompt.user)
        assertTrue("JSON" in prompt.system && "dishes" in prompt.system)
    }

    @Test
    fun aDishKeepsOnlyIfEveryIngredientIsInTheKitchen() {
        val dishes = listOf(
            DishSuggestion("Paneer tomato curry", listOf("paneer", "Tomato"), "Simmer paneer in a tomato gravy."),
            DishSuggestion("Palak paneer", listOf("spinach", "paneer"), "Needs spinach that is gone."),
            DishSuggestion("Jeera rice", listOf("basmati rice"), "Fry cumin, add rice and water."),
            DishSuggestion("Nothing", emptyList(), "No ingredients named."),
            DishSuggestion("  ", listOf("rice"), "No name."),
        )
        val kept = DishSuggestions.restrictToStock(dishes, items)
        assertEquals(listOf("Paneer tomato curry", "Jeera rice"), kept.map { it.name })
    }

    @Test
    fun ingredientsMatchWholeWordsNotFragments() {
        val dishes =
            listOf(DishSuggestion("Rice pudding", listOf("ric"), "x"), DishSuggestion("Rice", listOf("rice"), "y"))
        assertEquals(listOf("Rice"), DishSuggestions.restrictToStock(dishes, items).map { it.name })
    }

    @Test
    fun duplicatesAreDroppedAndTheListIsCapped() {
        val dish = DishSuggestion("Rice", listOf("rice"), "plain")
        assertEquals(1, DishSuggestions.restrictToStock(listOf(dish, dish.copy(name = "rice")), items).size)
        val many = List(10) { DishSuggestion("Rice $it", listOf("rice"), "plain") }
        assertEquals(DishSuggestions.MAX_DISHES, DishSuggestions.restrictToStock(many, items).size)
    }
}
