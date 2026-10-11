package app.rasoi.core.domain.bills

import app.rasoi.core.domain.household.Permission
import app.rasoi.core.domain.household.RolePolicy
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Bill
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Role
import app.rasoi.core.model.flatMap

/** Creates a bill from a title and lines typed in by hand, after the same checks the server makes. */
class CreateManualBillUseCase(private val repository: BillRepository) {
    suspend operator fun invoke(householdId: HouseholdId, myRole: Role, draft: ManualBillDraft): AppResult<Bill> =
        if (RolePolicy.allows(myRole, Permission.EDIT_BILLS)) {
            ManualBills.validate(draft).flatMap { repository.createManual(householdId, it) }
        } else {
            AppResult.Failure(AppError.Forbidden)
        }
}
