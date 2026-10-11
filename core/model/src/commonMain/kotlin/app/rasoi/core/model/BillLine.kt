package app.rasoi.core.model

/**
 * One purchased line. [amount] is what was paid for the whole line and is the split basis; [units] is the count
 * used by quantity splits; [measure] is a weight or pack size when the invoice gives one. [rawName] is as printed,
 * [displayName] is what the household sees and may edit.
 */
data class BillLine(
    val id: BillLineId,
    val rawName: String,
    val displayName: String,
    val units: Int,
    val amount: Money,
    val measure: Quantity? = null,
    val mrp: Money? = null,
    val hsn: String? = null,
    val upc: String? = null,
)
