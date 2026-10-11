package app.rasoi.core.model

/** A bill-level fee, discount or round-off; discounts are negative. */
data class BillCharge(val id: ChargeId, val kind: ChargeKind, val label: String, val amount: Money)
