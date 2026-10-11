package app.rasoi.core.contract.bills

import kotlinx.serialization.Serializable

/** How one line of the bill is shared. */
@Serializable
data class LineSplitEditDto(val lineId: String, val mode: SplitModeDto, val entries: List<SplitEntryDto>)
