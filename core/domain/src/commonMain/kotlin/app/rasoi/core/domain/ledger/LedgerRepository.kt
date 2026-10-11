package app.rasoi.core.domain.ledger

import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Ledger
import app.rasoi.core.model.LedgerTransfer
import app.rasoi.core.model.PeriodMonth
import app.rasoi.core.model.SettlementId

interface LedgerRepository {
    /** The ledger of [month], or of the open period when it is null. */
    suspend fun ledger(householdId: HouseholdId, month: PeriodMonth?): AppResult<Ledger>

    /** Closes the open period early (owners only); returns the closed ledger. */
    suspend fun closeMonth(householdId: HouseholdId, month: PeriodMonth): AppResult<Ledger>

    suspend fun markPaid(settlementId: SettlementId): AppResult<LedgerTransfer>

    /** The caller's own UPI address, if they gave one. */
    suspend fun myUpiVpa(): AppResult<String?>

    /** Sets the caller's UPI address, or clears it with null; returns what was stored. */
    suspend fun setUpiVpa(vpa: String?): AppResult<String?>
}
