package app.rasoi.core.domain.ledger

import app.rasoi.core.domain.household.Permission
import app.rasoi.core.domain.household.RolePolicy
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Ledger
import app.rasoi.core.model.PeriodMonth
import app.rasoi.core.model.Role

/** Cooks never see money, so the call is refused before it reaches the server. */
class GetLedgerUseCase(private val repository: LedgerRepository) {
    suspend operator fun invoke(householdId: HouseholdId, myRole: Role, month: PeriodMonth? = null): AppResult<Ledger> =
        if (RolePolicy.allows(myRole, Permission.VIEW_LEDGER)) {
            repository.ledger(householdId, month)
        } else {
            AppResult.Failure(AppError.Forbidden)
        }
}
