package app.rasoi.core.domain.bills

import app.rasoi.core.domain.split.ApprovalRules
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.ApprovalState
import app.rasoi.core.model.Bill
import app.rasoi.core.model.UserId
import app.rasoi.core.model.flatMap
import kotlin.time.Clock

/** Approves a bill or asks for changes; a change request needs a comment. */
class DecideBillUseCase(private val repository: BillRepository, private val clock: Clock) {
    suspend operator fun invoke(bill: Bill, me: UserId, state: ApprovalState, comment: String?): AppResult<Bill> =
        ApprovalRules.decide(bill, me, state, comment, clock.now())
            .flatMap { repository.decide(bill.id, state, comment?.trim()?.takeIf { it.isNotEmpty() }) }
}
