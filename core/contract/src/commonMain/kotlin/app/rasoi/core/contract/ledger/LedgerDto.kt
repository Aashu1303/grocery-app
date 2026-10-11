package app.rasoi.core.contract.ledger

import kotlinx.serialization.Serializable

/**
 * A period's balances and the transfers that settle them. [month] is `YYYY-MM`; [months] lists every period of the
 * household, newest first. [pendingBills] counts submitted bills that are not approved yet and so not in the balances.
 */
@Serializable
data class LedgerDto(
    val month: String,
    val status: PeriodStatusDto,
    val balances: List<BalanceDto>,
    val transfers: List<TransferDto>,
    val pendingBills: Int,
    val months: List<String>,
)
