package app.rasoi.core.domain.bills

import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillId

class GetBillUseCase(private val repository: BillRepository) {
    suspend operator fun invoke(id: BillId): AppResult<Bill> = repository.bill(id)
}
