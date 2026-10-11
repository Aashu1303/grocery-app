package app.rasoi.core.domain.pantry

import app.rasoi.core.model.Category
import app.rasoi.core.model.Platform
import kotlin.test.Test
import kotlin.test.assertEquals

class CategorizerTest {
    private fun category(name: String, hsn: String? = null, platform: Platform = Platform.ZEPTO) =
        Categorizer.categorize(name, hsn, platform).category

    @Test
    fun hsnChapterGivesTheCategory() {
        assertEquals(Category.PRODUCE, category("Item", "07101000"))
        assertEquals(Category.PRODUCE, category("Item", "08039010"))
        assertEquals(Category.DAIRY, category("Item", "04061000"))
        assertEquals(Category.DAIRY, category("Item", "04011000"))
        assertEquals(Category.EGGS_MEAT, category("Item", "04072100"))
        assertEquals(Category.EGGS_MEAT, category("Item", "0207"))
        assertEquals(Category.STAPLES, category("Item", "10063090"))
        assertEquals(Category.SNACKS, category("Item", "21069099"))
        assertEquals(Category.SNACKS, category("Item", "19059040"))
        assertEquals(Category.CONDIMENTS, category("Item", "21032000"))
        assertEquals(Category.FROZEN, category("Item", "21050000"))
        assertEquals(Category.BEVERAGES, category("Item", "22029920"))
        assertEquals(Category.BEVERAGES, category("Item", "22021010"))
        assertEquals(Category.HOUSEHOLD, category("Item", "39269099"))
        assertEquals(Category.HOUSEHOLD, category("Item", "49011020"))
        assertEquals(Category.PERSONAL_CARE, category("Item", "33051090"))
    }

    @Test
    fun keywordsOverrideTheHsnChapter() {
        assertEquals(Category.DAIRY, category("Amul Lactose Free Milk", "22029920"))
        assertEquals(Category.PERSONAL_CARE, category("Nat Habit Shampoo Flyer", "49011020"))
        assertEquals(Category.SNACKS, category("Too Yumm Bhujia Sev Snacks", "21069099"))
        assertEquals(Category.CONDIMENTS, category("Kissan Sweet & Spicy Ketchup Doy Bottle", "21032000"))
    }

    @Test
    fun namesAloneAreEnoughWhenThereIsNoHsn() {
        mapOf(
            "Desi Farms Low Fat Dahi Pouch" to Category.DAIRY,
            "Akshayakalpa Organic High Protein Paneer" to Category.DAIRY,
            "Brown Eggs Pack of 6" to Category.EGGS_MEAT,
            "Peanut Butter Crunchy" to Category.CONDIMENTS,
            "Robusta Banana (Keli)" to Category.PRODUCE,
            "Potato 900 g - 1 kg" to Category.PRODUCE,
            "Lemon 200 g" to Category.PRODUCE,
            "Whole Wheat Bread" to Category.BAKERY,
            "Tata Salt" to Category.STAPLES,
            "Toor Dal 1 kg" to Category.STAPLES,
            "Diet Coke Can - Cola Sparkling Soft Drink" to Category.BEVERAGES,
            "Vanilla Ice Cream Tub" to Category.FROZEN,
            "Surf Excel Detergent Powder" to Category.HOUSEHOLD,
            "Colgate Toothpaste" to Category.PERSONAL_CARE,
        ).forEach { (name, expected) -> assertEquals(expected, category(name), name) }
    }

    @Test
    fun keywordsMatchWholeWordsOnly() {
        assertEquals(Category.OTHER, category("Eggplant Cutlery Set"))
        assertEquals(Category.OTHER, category("Cheesecake Mould"))
        assertEquals(Category.PRODUCE, category("Eggplant (Brinjal)"))
    }

    @Test
    fun handpickdBillsAreProduceByDefault() {
        assertEquals(Category.PRODUCE, category("Bitter Gourd Green-Small", platform = Platform.HANDPICKD))
        assertEquals(Category.PRODUCE, category("Colocasia (Arbi)-Roots", platform = Platform.HANDPICKD))
    }

    @Test
    fun unknownItemsAreNotFoodUntilTheUserSaysSo() {
        val result = Categorizer.categorize("Mystery Item 42", null, Platform.MANUAL)
        assertEquals(Category.OTHER, result.category)
        assertEquals(false, result.isFood)
    }

    @Test
    fun foodFollowsTheCategory() {
        assertEquals(true, Categorizer.categorize("Paneer", null, Platform.ZEPTO).isFood)
        assertEquals(false, Categorizer.categorize("Shampoo", null, Platform.ZEPTO).isFood)
    }

    @Test
    fun theSampleInvoicesCategorizeSensibly() {
        mapOf(
            Triple("Too Yumm Kashmiri Chilli Potato Chips, 40% Less Saturated Fats", "21069099", Platform.INSTAMART) to
                Category.SNACKS,
            Triple("PapaNata Musk Mellon Drink", "22029920", Platform.INSTAMART) to Category.BEVERAGES,
            Triple("Flyer - Nat Habit Shampoo", "49011020", Platform.INSTAMART) to Category.PERSONAL_CARE,
            Triple("Boldfit Reusable Hot / Cold Pack", "39269099", Platform.BLINKIT) to Category.HOUSEHOLD,
            Triple("Lijjat Papad - Punjabi Masala", "19059040", Platform.ZEPTO) to Category.SNACKS,
            Triple("Country Delight 40g Protein Taaza Paneer", "04061000", Platform.ZEPTO) to Category.DAIRY,
        ).forEach { (input, expected) ->
            val (name, hsn, platform) = input
            assertEquals(expected, category(name, hsn, platform), name)
        }
    }
}
