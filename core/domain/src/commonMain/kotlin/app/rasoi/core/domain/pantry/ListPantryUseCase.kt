package app.rasoi.core.domain.pantry

import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.PantryStatus

class ListPantryUseCase(private val repository: PantryRepository) {
    suspend operator fun invoke(
        householdId: HouseholdId,
        status: PantryStatus? = PantryStatus.IN_STOCK,
    ): AppResult<List<PantryItem>> = repository.items(householdId, status)
}
