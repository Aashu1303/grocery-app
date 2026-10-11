package app.rasoi.server.service

import app.rasoi.core.domain.household.Permission
import app.rasoi.core.domain.pantry.ExpiryRules
import app.rasoi.core.domain.pantry.NewPantryItem
import app.rasoi.core.domain.pantry.PantryEdit
import app.rasoi.core.domain.pantry.PantryRules
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.LocationId
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.PantryItemId
import app.rasoi.core.model.PantryStatus
import app.rasoi.core.model.UserId
import app.rasoi.core.model.flatMap
import app.rasoi.core.model.map
import app.rasoi.server.persistence.HouseholdStore
import app.rasoi.server.persistence.LocationStore
import app.rasoi.server.persistence.PantryStore
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.uuid.Uuid

/**
 * Reading and changing a household's pantry. Cooks may edit it (design-plan 3: they run the kitchen), non-members
 * get `NotFound`, and an item or storage place from another household is never revealed.
 */
class PantryService(
    private val households: HouseholdService,
    private val householdStore: HouseholdStore,
    private val pantry: PantryStore,
    private val locations: LocationStore,
    private val clock: Clock,
) {
    suspend fun list(userId: UserId, householdId: HouseholdId, status: PantryStatus?): AppResult<List<PantryItem>> =
        households.authorize(userId, householdId, Permission.VIEW_PANTRY).map { pantry.list(householdId, status) }

    suspend fun add(userId: UserId, householdId: HouseholdId, request: NewPantryItem): AppResult<PantryItem> =
        households.authorize(userId, householdId, Permission.EDIT_PANTRY)
            .flatMap { PantryRules.validate(request) }
            .flatMap { checkLocation(householdId, it.locationId) }
            .map {
                val today = today(householdId)
                val item = PantryItem(
                    id = PantryItemId(Uuid.random().toString()),
                    householdId = householdId,
                    name = request.name.trim(),
                    category = request.category,
                    quantity = request.quantity,
                    locationId = request.locationId,
                    expiresOn = request.expiresOn ?: ExpiryRules.estimate(request.name, request.category, today),
                    status = PantryStatus.IN_STOCK,
                    addedOn = today,
                )
                pantry.insert(item, clock.now())
                item
            }

    suspend fun update(
        userId: UserId,
        householdId: HouseholdId,
        itemId: PantryItemId,
        edit: PantryEdit,
    ): AppResult<PantryItem> = households.authorize(userId, householdId, Permission.EDIT_PANTRY)
        .flatMap { find(householdId, itemId) }
        .flatMap { item -> checkLocation(householdId, edit.locationId).flatMap { PantryRules.apply(item, edit) } }
        .map { pantry.save(it, clock.now()) }

    private suspend fun find(householdId: HouseholdId, itemId: PantryItemId): AppResult<PantryItem> {
        val item = pantry.find(itemId)
        return if (item != null && item.householdId == householdId) {
            AppResult.Success(item)
        } else {
            AppResult.Failure(AppError.NotFound("pantry item"))
        }
    }

    private suspend fun checkLocation(householdId: HouseholdId, locationId: LocationId?): AppResult<Unit> {
        val known = locationId == null || locations.list(householdId).any { it.id == locationId }
        return if (known) {
            AppResult.Success(Unit)
        } else {
            AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("locationId", "unknown"))))
        }
    }

    private suspend fun today(householdId: HouseholdId) = clock.now()
        .toLocalDateTime(requireNotNull(householdStore.find(householdId)) { "Member of a missing household" }.timeZone)
        .date
}
