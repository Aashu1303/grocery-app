package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

/** What one member owes for one line; a line's allocations sum to its amount. */
@Serializable
data class AllocationDto(
    val lineId: String,
    val userId: String,
    val mode: SplitModeDto,
    val units: Int,
    val amountPaise: Long,
)
