package app.rasoi.core.domain.pantry

import app.rasoi.core.domain.household.Permission
import app.rasoi.core.domain.household.RolePolicy
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.Role
import app.rasoi.core.model.flatMap

class AddPantryItemUseCase(private val repository: PantryRepository) {
    suspend operator fun invoke(householdId: HouseholdId, myRole: Role, item: NewPantryItem): AppResult<PantryItem> {
        if (!RolePolicy.allows(myRole, Permission.EDIT_PANTRY)) return AppResult.Failure(AppError.Forbidden)
        return PantryRules.validate(item).flatMap { repository.add(householdId, it) }
    }
}
