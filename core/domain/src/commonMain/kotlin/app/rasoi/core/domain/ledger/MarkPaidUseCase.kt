package app.rasoi.core.domain.ledger

import app.rasoi.core.model.AppResult
import app.rasoi.core.model.LedgerTransfer
import app.rasoi.core.model.SettlementId

class MarkPaidUseCase(private val repository: LedgerRepository) {
    suspend operator fun invoke(settlementId: SettlementId): AppResult<LedgerTransfer> =
        repository.markPaid(settlementId)
}
