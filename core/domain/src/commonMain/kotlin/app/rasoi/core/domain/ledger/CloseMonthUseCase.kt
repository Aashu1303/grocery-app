package app.rasoi.core.domain.ledger

import app.rasoi.core.domain.household.Permission
import app.rasoi.core.domain.household.RolePolicy
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Ledger
import app.rasoi.core.model.PeriodMonth
import app.rasoi.core.model.Role

/** Closing a month early is for owners; everyone else is refused before the request is sent. */
class CloseMonthUseCase(private val repository: LedgerRepository) {
    suspend operator fun invoke(householdId: HouseholdId, myRole: Role, month: PeriodMonth): AppResult<Ledger> =
        if (RolePolicy.allows(myRole, Permission.CLOSE_PERIOD)) {
            repository.closeMonth(householdId, month)
        } else {
            AppResult.Failure(AppError.Forbidden)
        }
}
