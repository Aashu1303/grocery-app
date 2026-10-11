package app.rasoi.server.persistence

import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.Category
import app.rasoi.core.model.Money
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.PantryItemId
import app.rasoi.core.model.PantryStatus
import app.rasoi.core.model.Quantity
import app.rasoi.core.model.QuantityUnit
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** Pantry statements shared by [PantryStore] and the bill store, so a bill and its pantry items commit together. */
internal fun JdbcTransaction.insertPantryItems(items: List<PantryItem>, now: Instant) {
    PantryItemsTable.batchInsert(items) { item ->
        this[PantryItemsTable.id] = Uuid.parse(item.id.value)
        this[PantryItemsTable.householdId] = item.householdId.uuid()
        this[PantryItemsTable.name] = item.name
        this[PantryItemsTable.category] = item.category.name
        this[PantryItemsTable.quantityAmount] = item.quantity.amount
        this[PantryItemsTable.quantityUnit] = item.quantity.unit.name
        this[PantryItemsTable.locationId] = item.locationId?.let { Uuid.parse(it.value) }
        this[PantryItemsTable.expiresOn] = item.expiresOn
        this[PantryItemsTable.status] = item.status.name
        this[PantryItemsTable.addedOn] = item.addedOn
        this[PantryItemsTable.pricePaise] = item.price?.paise
        this[PantryItemsTable.payerId] = item.payerId?.uuid()
        this[PantryItemsTable.sourceLineId] = item.sourceLineId?.let { Uuid.parse(it.value) }
        this[PantryItemsTable.createdAt] = now
        this[PantryItemsTable.updatedAt] = now
    }
}

/** Source lines that already have a pantry item, whatever its status. */
internal fun JdbcTransaction.lineIdsWithPantryItems(lineIds: List<BillLineId>): Set<BillLineId> =
    PantryItemsTable.selectAll()
        .where { PantryItemsTable.sourceLineId inList lineIds.map { Uuid.parse(it.value) } }
        .mapNotNull { row -> row[PantryItemsTable.sourceLineId]?.toBillLineId() }
        .toSet()

/** Removes untouched (still in stock) items that came from [lineIds]; used and discarded items are history. */
internal fun JdbcTransaction.deleteInStockItemsFor(lineIds: List<BillLineId>) {
    PantryItemsTable.deleteWhere {
        (sourceLineId inList lineIds.map { Uuid.parse(it.value) }) and (status eq PantryStatus.IN_STOCK.name)
    }
}

internal fun ResultRow.toPantryItem() = PantryItem(
    id = PantryItemId(this[PantryItemsTable.id].toString()),
    householdId = this[PantryItemsTable.householdId].toHouseholdId(),
    name = this[PantryItemsTable.name],
    category = Category.valueOf(this[PantryItemsTable.category]),
    quantity = Quantity(
        this[PantryItemsTable.quantityAmount],
        QuantityUnit.valueOf(this[PantryItemsTable.quantityUnit]),
    ),
    locationId = this[PantryItemsTable.locationId]?.toLocationId(),
    expiresOn = this[PantryItemsTable.expiresOn],
    status = PantryStatus.valueOf(this[PantryItemsTable.status]),
    addedOn = this[PantryItemsTable.addedOn],
    price = this[PantryItemsTable.pricePaise]?.let(::Money),
    payerId = this[PantryItemsTable.payerId]?.toUserId(),
    sourceLineId = this[PantryItemsTable.sourceLineId]?.toBillLineId(),
)
