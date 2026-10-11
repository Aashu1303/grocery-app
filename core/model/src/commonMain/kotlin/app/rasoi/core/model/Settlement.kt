package app.rasoi.core.model

/** A transfer that closes a period, and whether it has been paid. */
data class Settlement(
    val id: SettlementId,
    val periodId: PeriodId,
    val transfer: Transfer,
    val status: SettlementStatus,
)
