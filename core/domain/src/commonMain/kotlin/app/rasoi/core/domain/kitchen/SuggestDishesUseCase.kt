package app.rasoi.core.domain.kitchen

import app.rasoi.core.domain.household.Permission
import app.rasoi.core.domain.household.RolePolicy
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Role

class SuggestDishesUseCase(private val repository: KitchenRepository) {
    suspend operator fun invoke(householdId: HouseholdId, myRole: Role): AppResult<List<DishSuggestion>> =
        if (RolePolicy.allows(myRole, Permission.VIEW_PANTRY)) {
            repository.suggestions(householdId)
        } else {
            AppResult.Failure(AppError.Forbidden)
        }
}
