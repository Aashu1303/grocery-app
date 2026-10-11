package app.rasoi.core.model

/**
 * One transfer in a ledger: a suggestion while the period is open, a stored settlement once it is closed
 * ([settlementId] and [status] are null for suggestions). [payeeUpiVpa] is where the receiver takes UPI payments.
 */
data class LedgerTransfer(
    val transfer: Transfer,
    val settlementId: SettlementId? = null,
    val status: SettlementStatus? = null,
    val payeeUpiVpa: String? = null,
)

/**
 * A period's balances and the transfers that settle them. [pendingBills] counts submitted bills that are not
 * approved yet and so are not in the balances; [months] lists the household's periods, newest first.
 */
data class Ledger(
    val month: PeriodMonth,
    val status: PeriodStatus,
    val balances: List<Balance>,
    val transfers: List<LedgerTransfer>,
    val pendingBills: Int,
    val months: List<PeriodMonth>,
)
