package app.rasoi.core.domain.kitchen

import app.rasoi.core.model.Category
import app.rasoi.core.model.PantryItemId
import app.rasoi.core.model.PantryStatus
import app.rasoi.core.model.Quantity
import app.rasoi.core.testing.Builders
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ExpiryAlertsTest {
    private val today = LocalDate(2026, 10, 11)

    private fun item(
        id: String,
        expires: LocalDate?,
        category: Category = Category.DAIRY,
        status: PantryStatus = PantryStatus.IN_STOCK,
    ) = Builders.pantryItem(id, "Item $id", category, Quantity.count(1), "l1", expires, status)

    private fun alerts(
        sent: Set<Pair<PantryItemId, ExpiryAlertKind>> = emptySet(),
        vararg items: app.rasoi.core.model.PantryItem,
    ) = ExpiryAlerts.due(items.toList(), today, sent).map { Triple(it.item.id.value, it.kind, it.daysLeft) }

    @Test
    fun itemsExpiringWithinTwoDaysGetAHeadsUpAndExpiredOnesAnExpiredAlert() {
        val result = alerts(
            emptySet(),
            item("soon", LocalDate(2026, 10, 12)),
            item("today", today),
            item("late", LocalDate(2026, 10, 9)),
            item("fine", LocalDate(2026, 10, 20)),
        )
        assertEquals(
            listOf(
                Triple("soon", ExpiryAlertKind.EXPIRES_SOON, 1),
                Triple("today", ExpiryAlertKind.EXPIRES_SOON, 0),
                Triple("late", ExpiryAlertKind.EXPIRED, -2),
            ),
            result,
        )
    }

    @Test
    fun eachKindIsSentOncePerItem() {
        val soon = item("soon", LocalDate(2026, 10, 12))
        val sent = setOf(soon.id to ExpiryAlertKind.EXPIRES_SOON)
        assertEquals(emptyList(), alerts(sent, soon))
        val lateSoon = item("soon", LocalDate(2026, 10, 10))
        assertEquals(listOf(Triple("soon", ExpiryAlertKind.EXPIRED, -1)), alerts(sent, lateSoon))
    }

    @Test
    fun finishedDiscardedUndatedAndNonFoodItemsAreIgnored() {
        val result = alerts(
            emptySet(),
            item("a", today, status = PantryStatus.FINISHED),
            item("b", today, status = PantryStatus.DISCARDED),
            item("c", null),
            item("d", today, category = Category.HOUSEHOLD),
        )
        assertEquals(emptyList(), result)
    }
}
