package app.rasoi.core.designsystem.catalog

import app.rasoi.core.designsystem.component.MemberOption

/** Sample data shown in the catalog. Fixture content, not product copy. */
internal object CatalogSamples {
    const val ITEM = "Amul Taaza Toned Milk"
    const val VEGETABLE = "Bottle Gourd"
    const val QUANTITY = "500 ml"
    const val LOCATION = "Fridge"
    const val TITLE = "Pantry"
    const val MESSAGE = "Add items from an invoice or by hand."
    const val ERROR = "Couldn't load your pantry. Check your connection."
    const val ACTION = "Import invoice"
    const val SPLIT_SUMMARY = "Split equally between 3"
    const val NON_FOOD = "Non-food"
    const val APPROVE = "Approve split"
    const val DELETE = "Delete bill"
    val members = listOf(MemberOption("u1", "Aarav Shah"), MemberOption("u2", "Meera Iyer"), MemberOption("u3", "Ravi"))
}
