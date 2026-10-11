package app.rasoi.server.parsing.model

import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.Money

/** A bill-level charge as printed; discounts are negative. */
data class ParsedCharge(val kind: ChargeKind, val label: String, val amount: Money)
