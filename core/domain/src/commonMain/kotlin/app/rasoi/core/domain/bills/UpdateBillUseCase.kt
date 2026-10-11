package app.rasoi.core.domain.bills

import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillId
import app.rasoi.core.model.flatMap

/** Validates an edit with the same [BillRules] the server applies, so field errors show without a round trip. */
class UpdateBillUseCase(private val repository: BillRepository) {
    suspend operator fun invoke(id: BillId, edit: BillEdit): AppResult<Bill> =
        BillRules.validate(edit).flatMap { repository.update(id, it) }
}
