package app.rasoi.core.model

/** How one bill line is shared (design-plan 6.4 item 1). */
data class LineSplit(val mode: SplitMode, val entries: List<SplitEntry>)
