package app.rasoi.core.domain.pantry

import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.PantryItemId
import app.rasoi.core.model.PantryStatus

interface PantryRepository {
    /** Items with [status]; every status when it is null. */
    suspend fun items(householdId: HouseholdId, status: PantryStatus?): AppResult<List<PantryItem>>

    suspend fun add(householdId: HouseholdId, item: NewPantryItem): AppResult<PantryItem>

    suspend fun update(householdId: HouseholdId, id: PantryItemId, edit: PantryEdit): AppResult<PantryItem>
}
