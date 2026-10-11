package app.rasoi.core.domain.bills

import app.rasoi.core.domain.split.ApprovalRules
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Bill
import app.rasoi.core.model.UserId
import app.rasoi.core.model.flatMap
import kotlin.time.Clock

/** Sends a bill for approval; refuses locally when it is not fully split or its total does not add up. */
class SubmitBillUseCase(private val repository: BillRepository, private val clock: Clock) {
    suspend operator fun invoke(bill: Bill, me: UserId): AppResult<Bill> =
        ApprovalRules.submit(bill, me, clock.now()).flatMap { repository.submit(bill.id) }
}
