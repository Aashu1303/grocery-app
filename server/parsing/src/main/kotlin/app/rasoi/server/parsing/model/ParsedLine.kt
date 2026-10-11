package app.rasoi.server.parsing.model

import app.rasoi.core.model.Money
import app.rasoi.core.model.Quantity

/**
 * One purchased line as printed. [amount] is what was actually paid for the whole line (the split basis,
 * ADR 0017); [units] is the count column; [measure] is a measured weight or a pack size per unit, when known.
 */
data class ParsedLine(
    val rawName: String,
    val units: Int,
    val amount: Money,
    val measure: Quantity? = null,
    val mrp: Money? = null,
    val hsn: String? = null,
    val upc: String? = null,
)
