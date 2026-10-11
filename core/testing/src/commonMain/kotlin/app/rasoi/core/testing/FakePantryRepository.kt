package app.rasoi.core.testing

import app.rasoi.core.domain.pantry.NewPantryItem
import app.rasoi.core.domain.pantry.PantryEdit
import app.rasoi.core.domain.pantry.PantryRepository
import app.rasoi.core.domain.pantry.PantryRules
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.PantryItemId
import app.rasoi.core.model.PantryStatus
import app.rasoi.core.model.flatMap
import kotlinx.datetime.LocalDate

/** In-memory pantry; set [failure] to make every call fail with that error. */
class FakePantryRepository(initial: List<PantryItem> = emptyList()) : PantryRepository {
    var failure: AppError? = null
    val items = initial.toMutableList()
    val added = mutableListOf<NewPantryItem>()
    val updates = mutableListOf<Pair<PantryItemId, PantryEdit>>()
    private var nextId = 0

    override suspend fun items(householdId: HouseholdId, status: PantryStatus?): AppResult<List<PantryItem>> =
        result { AppResult.Success(items.filter { status == null || it.status == status }) }

    override suspend fun add(householdId: HouseholdId, item: NewPantryItem): AppResult<PantryItem> = result {
        added += item
        val created = Builders.pantryItem(
            id = "new${nextId++}",
            name = item.name,
            category = item.category,
            quantity = item.quantity,
            locationId = item.locationId?.value,
            expiresOn = item.expiresOn ?: LocalDate.parse("2026-10-10"),
        )
        items += created
        AppResult.Success(created)
    }

    override suspend fun update(householdId: HouseholdId, id: PantryItemId, edit: PantryEdit): AppResult<PantryItem> =
        result {
            updates += id to edit
            val index = items.indexOfFirst { it.id == id }
            if (index < 0) {
                AppResult.Failure(AppError.NotFound("pantry item"))
            } else {
                PantryRules.apply(items[index], edit).flatMap { updated ->
                    items[index] = updated
                    AppResult.Success(updated)
                }
            }
        }

    private inline fun <T> result(block: () -> AppResult<T>): AppResult<T> =
        failure?.let { AppResult.Failure(it) } ?: block()
}
