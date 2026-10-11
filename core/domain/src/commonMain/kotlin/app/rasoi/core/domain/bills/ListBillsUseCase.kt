package app.rasoi.core.domain.bills

import app.rasoi.core.domain.household.Permission
import app.rasoi.core.domain.household.RolePolicy
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.BillSummary
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Role

/** Cooks never see money, so the call is refused before it reaches the server. */
class ListBillsUseCase(private val repository: BillRepository) {
    suspend operator fun invoke(householdId: HouseholdId, myRole: Role): AppResult<List<BillSummary>> =
        if (RolePolicy.allows(myRole, Permission.VIEW_BILLS)) {
            repository.bills(householdId)
        } else {
            AppResult.Failure(AppError.Forbidden)
        }
}
