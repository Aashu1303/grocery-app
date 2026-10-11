package app.rasoi.core.domain.pantry

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Category
import app.rasoi.core.model.LocationId
import app.rasoi.core.model.Money
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.PantryStatus
import app.rasoi.core.model.Quantity
import app.rasoi.core.model.QuantityUnit
import app.rasoi.core.testing.Builders
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PantryRulesTest {
    private val item = Builders.pantryItem()

    private fun violations(result: AppResult<*>): List<Pair<String, String>> =
        assertIs<AppError.Validation>(assertIs<AppResult.Failure>(result).error).violations.map { it.field to it.code }

    @Test
    fun aLinesQuantityIsItsMeasureTimesItsUnits() {
        val milk = Builders.line(name = "Milk", units = 2).copy(measure = Quantity.millilitres(500))
        assertEquals(Quantity.millilitres(1_000), PantryRules.quantityFor(milk))
        val beans = Builders.line(name = "Beans", units = 1).copy(measure = Quantity.grams(360))
        assertEquals(Quantity.grams(360), PantryRules.quantityFor(beans))
        assertEquals(Quantity.count(3), PantryRules.quantityFor(Builders.line(name = "Lemon", units = 3)))
    }

    @Test
    fun aNewManualItemNeedsANameAndAPositiveQuantity() {
        val draft = NewPantryItem("Rice", Category.STAPLES, Quantity.kilograms(5), LocationId("l3"), null)
        assertEquals(AppResult.Success(draft), PantryRules.validate(draft))
        assertEquals(listOf("name" to "required"), violations(PantryRules.validate(draft.copy(name = " "))))
        assertEquals(
            listOf("name" to "too_long"),
            violations(PantryRules.validate(draft.copy(name = "x".repeat(PantryRules.MAX_NAME_LENGTH + 1)))),
        )
        assertEquals(
            listOf("quantity" to "required"),
            violations(PantryRules.validate(draft.copy(quantity = Quantity.zero(QuantityUnit.COUNT)))),
        )
    }

    @Test
    fun editsChangeOnlyWhatTheyName() {
        val edit = PantryEdit(name = " Brown rice ", quantity = Quantity.kilograms(2), locationId = LocationId("l9"))
        val updated = assertIs<AppResult.Success<PantryItem>>(PantryRules.apply(item, edit)).value
        assertEquals("Brown rice", updated.name)
        assertEquals(Quantity.kilograms(2), updated.quantity)
        assertEquals(LocationId("l9"), updated.locationId)
        assertEquals(item.expiresOn, updated.expiresOn)
        assertEquals(item.status, updated.status)
    }

    @Test
    fun anExpiryCanBeSetOrCleared() {
        val set = PantryEdit(expiresOn = LocalDate(2026, 11, 1))
        assertEquals(LocalDate(2026, 11, 1), (PantryRules.apply(item, set) as AppResult.Success).value.expiresOn)
        val cleared = PantryRules.apply(item.copy(expiresOn = LocalDate(2026, 11, 1)), PantryEdit(clearExpiry = true))
        assertEquals(null, (cleared as AppResult.Success).value.expiresOn)
        assertEquals(
            listOf("expiresOn" to "conflict"),
            violations(PantryRules.apply(item, PantryEdit(expiresOn = LocalDate(2026, 11, 1), clearExpiry = true))),
        )
    }

    @Test
    fun finishingOrDiscardingChangesTheStatus() {
        val finished = PantryRules.apply(item, PantryEdit(status = PantryStatus.FINISHED))
        assertEquals(PantryStatus.FINISHED, (finished as AppResult.Success).value.status)
    }

    @Test
    fun invalidAndEmptyEditsAreRejected() {
        assertEquals(listOf("body" to "empty"), violations(PantryRules.apply(item, PantryEdit())))
        assertEquals(listOf("name" to "required"), violations(PantryRules.apply(item, PantryEdit(name = ""))))
        assertEquals(
            listOf("quantity" to "required"),
            violations(PantryRules.apply(item, PantryEdit(quantity = Quantity.zero(QuantityUnit.MILLIGRAM)))),
        )
    }

    @Test
    fun anItemFromABillLineKeepsItsPriceAndSource() {
        val line = Builders.line(id = "l1", name = "Paneer", amount = 17_600).copy(category = Category.DAIRY)
        val created = PantryRules.fromLine(
            line = line,
            bill = Builders.bill(lines = listOf(line)),
            locationId = LocationId("fridge"),
            expiresOn = LocalDate(2026, 10, 9),
            id = item.id,
        )
        assertEquals(Category.DAIRY, created.category)
        assertEquals(Money(17_600), created.price)
        assertEquals(line.id, created.sourceLineId)
        assertEquals(LocalDate(2026, 10, 4), created.addedOn)
        assertEquals(PantryStatus.IN_STOCK, created.status)
    }
}
