package app.rasoi.server.persistence

import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.PantryItemId
import app.rasoi.core.model.PantryStatus
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Instant
import kotlin.uuid.Uuid

class PantryStore(private val db: RasoiDatabase) {
    suspend fun list(householdId: HouseholdId, status: PantryStatus?): List<PantryItem> = db.query {
        val query = PantryItemsTable.selectAll().where {
            val inHousehold = PantryItemsTable.householdId eq householdId.uuid()
            if (status == null) inHousehold else inHousehold and (PantryItemsTable.status eq status.name)
        }
        query.orderBy(PantryItemsTable.name to SortOrder.ASC, PantryItemsTable.addedOn to SortOrder.DESC)
            .map { it.toPantryItem() }
    }

    suspend fun find(id: PantryItemId): PantryItem? = db.query {
        PantryItemsTable.selectAll().where {
            PantryItemsTable.id eq Uuid.parse(id.value)
        }.singleOrNull()?.toPantryItem()
    }

    suspend fun insert(item: PantryItem, now: Instant) = db.query { insertPantryItems(listOf(item), now) }

    suspend fun save(item: PantryItem, now: Instant): PantryItem = db.query {
        PantryItemsTable.update({ PantryItemsTable.id eq Uuid.parse(item.id.value) }) {
            it[name] = item.name
            it[quantityAmount] = item.quantity.amount
            it[quantityUnit] = item.quantity.unit.name
            it[locationId] = item.locationId?.let { id -> Uuid.parse(id.value) }
            it[expiresOn] = item.expiresOn
            it[status] = item.status.name
            it[updatedAt] = now
        }
        item
    }

    suspend fun lineIdsWithItems(lineIds: List<BillLineId>): Set<BillLineId> =
        if (lineIds.isEmpty()) emptySet() else db.query { lineIdsWithPantryItems(lineIds) }
}
