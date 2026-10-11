package app.rasoi.server.service

import app.rasoi.core.model.Balance
import app.rasoi.core.model.Period
import app.rasoi.core.model.PeriodMonth
import app.rasoi.core.model.SettlementId
import app.rasoi.core.model.SettlementStatus
import app.rasoi.core.model.Transfer

/** A transfer in a ledger: a suggestion in an open period, a stored settlement once it is closed. */
data class LedgerTransfer(
    val transfer: Transfer,
    val settlementId: SettlementId? = null,
    val status: SettlementStatus? = null,
    val payeeUpiVpa: String? = null,
)

/** A period's balances and the transfers that settle them. */
data class Ledger(
    val period: Period,
    val balances: List<Balance>,
    val transfers: List<LedgerTransfer>,
    val pendingBills: Int,
    val months: List<PeriodMonth>,
)
