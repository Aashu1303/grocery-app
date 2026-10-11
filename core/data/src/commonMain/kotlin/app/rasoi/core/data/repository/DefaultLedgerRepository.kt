package app.rasoi.core.data.repository

import app.rasoi.core.contract.auth.UpdateProfileRequestDto
import app.rasoi.core.data.network.RasoiApi
import app.rasoi.core.data.network.closeMonth
import app.rasoi.core.data.network.ledger
import app.rasoi.core.data.network.markPaid
import app.rasoi.core.data.network.updateProfile
import app.rasoi.core.domain.ledger.LedgerRepository
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Ledger
import app.rasoi.core.model.LedgerTransfer
import app.rasoi.core.model.PeriodMonth
import app.rasoi.core.model.SettlementId
import app.rasoi.core.model.map

class DefaultLedgerRepository(private val api: RasoiApi) : LedgerRepository {
    override suspend fun ledger(householdId: HouseholdId, month: PeriodMonth?): AppResult<Ledger> =
        api.ledger(householdId.value, month?.toString()).map { it.toModel() }

    override suspend fun closeMonth(householdId: HouseholdId, month: PeriodMonth): AppResult<Ledger> =
        api.closeMonth(householdId.value, month.toString()).map { it.toModel() }

    override suspend fun markPaid(settlementId: SettlementId): AppResult<LedgerTransfer> =
        api.markPaid(settlementId.value).map { it.toModel() }

    override suspend fun myUpiVpa(): AppResult<String?> = api.me().map { it.user.upiVpa }

    override suspend fun setUpiVpa(vpa: String?): AppResult<String?> =
        api.updateProfile(UpdateProfileRequestDto(vpa)).map { it.upiVpa }
}
