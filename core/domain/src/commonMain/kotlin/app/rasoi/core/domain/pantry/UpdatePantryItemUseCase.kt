package app.rasoi.core.domain.pantry

import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.PantryItemId

/** Rejects an empty or invalid edit locally; the server applies the same [PantryRules]. */
class UpdatePantryItemUseCase(private val repository: PantryRepository) {
    suspend operator fun invoke(householdId: HouseholdId, id: PantryItemId, edit: PantryEdit): AppResult<PantryItem> {
        val problem = PantryRules.problems(edit)
        if (problem != null) return AppResult.Failure(problem)
        return repository.update(householdId, id, edit)
    }
}
