package app.rasoi.core.model

/** What one member owes for one line. [amount] is stored after computation; a line's allocations sum to its amount. */
data class Allocation(
    val lineId: BillLineId,
    val userId: UserId,
    val mode: SplitMode,
    val units: Int,
    val amount: Money,
)
