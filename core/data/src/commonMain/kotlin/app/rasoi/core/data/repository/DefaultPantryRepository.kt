package app.rasoi.core.data.repository

import app.rasoi.core.data.network.RasoiApi
import app.rasoi.core.domain.pantry.NewPantryItem
import app.rasoi.core.domain.pantry.PantryEdit
import app.rasoi.core.domain.pantry.PantryRepository
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.PantryItemId
import app.rasoi.core.model.PantryStatus
import app.rasoi.core.model.map

class DefaultPantryRepository(private val api: RasoiApi) : PantryRepository {
    override suspend fun items(householdId: HouseholdId, status: PantryStatus?): AppResult<List<PantryItem>> =
        api.pantry(householdId.value, status?.name ?: "ALL").map { items -> items.map { it.toModel(householdId) } }

    override suspend fun add(householdId: HouseholdId, item: NewPantryItem): AppResult<PantryItem> =
        api.addPantryItem(householdId.value, item.toDto()).map { it.toModel(householdId) }

    override suspend fun update(householdId: HouseholdId, id: PantryItemId, edit: PantryEdit): AppResult<PantryItem> =
        api.updatePantryItem(householdId.value, id.value, edit.toDto()).map { it.toModel(householdId) }
}
