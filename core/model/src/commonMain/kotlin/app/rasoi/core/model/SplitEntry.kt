package app.rasoi.core.model

/** One member's part of a line split; [units] only matters for [SplitMode.QUANTITY]. */
data class SplitEntry(val userId: UserId, val units: Int = 1)
