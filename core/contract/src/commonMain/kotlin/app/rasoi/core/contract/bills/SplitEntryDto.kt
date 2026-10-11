package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

/** One member's part of a line split; [units] only matters for the QUANTITY mode. */
@Serializable
data class SplitEntryDto(val userId: String, val units: Int = 1)
