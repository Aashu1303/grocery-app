package app.rasoi.core.domain.kitchen

import app.rasoi.core.model.Category
import app.rasoi.core.model.Freshness
import app.rasoi.core.model.PantryStatus
import app.rasoi.core.model.Quantity
import app.rasoi.core.testing.Builders
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class UseFirstTest {
    private val today = LocalDate(2026, 10, 11)

    private fun item(
        id: String,
        name: String,
        expires: LocalDate?,
        quantity: Quantity = Quantity.count(1),
        category: Category = Category.PRODUCE,
        status: PantryStatus = PantryStatus.IN_STOCK,
    ) = Builders.pantryItem(id, name, category, quantity, "l1", expires, status)

    @Test
    fun soonestExpiryComesFirstAndUndatedItemsLast() {
        val ranked = UseFirst.rank(
            listOf(
                item("a", "Rice", null, category = Category.STAPLES),
                item("b", "Beans", LocalDate(2026, 10, 14)),
                item("c", "Milk", LocalDate(2026, 10, 9), category = Category.DAIRY),
                item("d", "Paneer", LocalDate(2026, 10, 11), category = Category.DAIRY),
            ),
            today,
        )
        assertEquals(listOf("Milk", "Paneer", "Beans", "Rice"), ranked.map { it.item.name })
        assertEquals(listOf(-2, 0, 3, null), ranked.map { it.daysLeft })
        assertEquals(
            listOf(Freshness.EXPIRED, Freshness.USE_SOON, Freshness.FRESH, Freshness.FRESH),
            ranked.map { it.freshness },
        )
    }

    @Test
    fun theSameDayIsBrokenBySmallerQuantityThenName() {
        val day = LocalDate(2026, 10, 12)
        val ranked = UseFirst.rank(
            listOf(
                item("a", "Spinach", day, Quantity.grams(500)),
                item("b", "Beans", day, Quantity.grams(200)),
                item("c", "Okra", day, Quantity.grams(200)),
            ),
            today,
        )
        assertEquals(listOf("Beans", "Okra", "Spinach"), ranked.map { it.item.name })
    }

    @Test
    fun onlyFoodStillInStockIsRanked() {
        val ranked = UseFirst.rank(
            listOf(
                item("a", "Dishwash", LocalDate(2026, 10, 12), category = Category.HOUSEHOLD),
                item("b", "Old milk", LocalDate(2026, 10, 1), status = PantryStatus.DISCARDED),
                item("c", "Finished dal", LocalDate(2026, 10, 1), status = PantryStatus.FINISHED),
                item("d", "Eggs", LocalDate(2026, 10, 12), category = Category.EGGS_MEAT),
            ),
            today,
        )
        assertEquals(listOf("Eggs"), ranked.map { it.item.name })
    }

    @Test
    fun anEmptyPantryHasNothingToUseFirst() {
        assertEquals(emptyList(), UseFirst.rank(emptyList(), today))
    }
}
