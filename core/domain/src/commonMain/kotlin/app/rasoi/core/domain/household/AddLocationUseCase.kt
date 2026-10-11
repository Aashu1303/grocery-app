package app.rasoi.core.domain.household

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Location
import app.rasoi.core.model.LocationKind
import app.rasoi.core.model.Role
import app.rasoi.core.model.flatMap

class AddLocationUseCase(private val repository: HouseholdRepository) {
    suspend operator fun invoke(
        householdId: HouseholdId,
        myRole: Role,
        name: String,
        kind: LocationKind,
    ): AppResult<Location> {
        if (!RolePolicy.allows(myRole, Permission.MANAGE_LOCATIONS)) return AppResult.Failure(AppError.Forbidden)
        return HouseholdRules.validateLocationName(name).flatMap { repository.addLocation(householdId, it, kind) }
    }
}
