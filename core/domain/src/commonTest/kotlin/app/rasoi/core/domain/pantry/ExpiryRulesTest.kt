package app.rasoi.core.domain.pantry

import app.rasoi.core.model.Category
import app.rasoi.core.model.Freshness
import app.rasoi.core.model.LocationKind
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExpiryRulesTest {
    private val bought = LocalDate(2026, 10, 4)

    private fun expiry(name: String, category: Category) = ExpiryRules.estimate(name, category, bought)

    @Test
    fun shelfLifeComesFromTheCategoryUnlessTheNameIsMoreSpecific() {
        assertEquals(LocalDate(2026, 10, 9), expiry("Beans Lobia", Category.PRODUCE))
        assertEquals(LocalDate(2026, 10, 6), expiry("Amul Taaza Toned Milk", Category.DAIRY))
        assertEquals(LocalDate(2026, 10, 9), expiry("Amul Paneer", Category.DAIRY))
        assertEquals(LocalDate(2026, 10, 7), expiry("Palak (Spinach)", Category.PRODUCE))
        assertEquals(LocalDate(2026, 10, 25), expiry("Brown Eggs Pack of 6", Category.EGGS_MEAT))
        assertEquals(LocalDate(2026, 10, 6), expiry("Chicken Curry Cut", Category.EGGS_MEAT))
        assertEquals(LocalDate(2026, 10, 7), expiry("Whole Wheat Bread", Category.BAKERY))
    }

    @Test
    fun longLivedFoodsGetLongShelfLives() {
        assertEquals(LocalDate(2027, 4, 2), expiry("Toor Dal", Category.STAPLES))
        assertEquals(LocalDate(2027, 1, 2), expiry("Chips", Category.SNACKS))
        assertEquals(LocalDate(2027, 4, 2), expiry("Ketchup", Category.CONDIMENTS))
    }

    @Test
    fun nonFoodHasNoExpiry() {
        assertNull(expiry("Shampoo", Category.PERSONAL_CARE))
        assertNull(expiry("Detergent", Category.HOUSEHOLD))
        assertNull(expiry("Mystery", Category.OTHER))
    }

    @Test
    fun defaultStorageFollowsTheCategory() {
        assertEquals(LocationKind.FRIDGE, ExpiryRules.defaultLocation("Amul Milk", Category.DAIRY))
        assertEquals(LocationKind.FRIDGE, ExpiryRules.defaultLocation("Beans", Category.PRODUCE))
        assertEquals(LocationKind.SHELF, ExpiryRules.defaultLocation("Potato 1 kg", Category.PRODUCE))
        assertEquals(LocationKind.SHELF, ExpiryRules.defaultLocation("Onion", Category.PRODUCE))
        assertEquals(LocationKind.FREEZER, ExpiryRules.defaultLocation("Ice cream", Category.FROZEN))
        assertEquals(LocationKind.SHELF, ExpiryRules.defaultLocation("Toor Dal", Category.STAPLES))
        assertEquals(LocationKind.FRIDGE, ExpiryRules.defaultLocation("Eggs", Category.EGGS_MEAT))
    }

    @Test
    fun freshnessIsRelativeToToday() {
        val expires = LocalDate(2026, 10, 10)
        assertEquals(Freshness.FRESH, ExpiryRules.freshness(expires, LocalDate(2026, 10, 6)))
        assertEquals(Freshness.USE_SOON, ExpiryRules.freshness(expires, LocalDate(2026, 10, 8)))
        assertEquals(Freshness.USE_SOON, ExpiryRules.freshness(expires, LocalDate(2026, 10, 10)))
        assertEquals(Freshness.EXPIRED, ExpiryRules.freshness(expires, LocalDate(2026, 10, 11)))
    }

    @Test
    fun daysLeftIsNegativeOncePastExpiry() {
        assertEquals(3, ExpiryRules.daysLeft(LocalDate(2026, 10, 10), LocalDate(2026, 10, 7)))
        assertEquals(0, ExpiryRules.daysLeft(LocalDate(2026, 10, 10), LocalDate(2026, 10, 10)))
        assertEquals(-2, ExpiryRules.daysLeft(LocalDate(2026, 10, 10), LocalDate(2026, 10, 12)))
    }
}
