package app.rasoi.server.parsing.layout

/** How a table cell's wrapped text sits relative to the row that carries the numbers. */
enum class CellAlignment { CENTERED, TOP }

/** A table row: the line with the numbers, and the full wrapped description. */
data class TableRow(val anchor: Line, val description: String)

/**
 * Rebuilds wrapped table rows (design-plan section 2). [lines] are the table body in reading order; anchors are
 * the lines that carry an item's numbers. Description fragments between anchors are split so that each item's
 * text is vertically centred on its anchor ([CellAlignment.CENTERED]: as many fragments below as above) or
 * all belong to the anchor above ([CellAlignment.TOP]).
 */
object DescriptionAssigner {
    fun assign(
        lines: List<Line>,
        isAnchor: (Line) -> Boolean,
        description: (Line) -> String,
        alignment: CellAlignment,
    ): List<TableRow> {
        val anchors = mutableListOf<Line>()
        val gaps = mutableListOf(mutableListOf<String>())
        lines.forEach { line ->
            if (isAnchor(line)) {
                anchors += line
                gaps += mutableListOf<String>()
            } else {
                description(line).takeIf { it.isNotBlank() }?.let { gaps.last() += it }
            }
        }
        var above: List<String> = if (alignment == CellAlignment.CENTERED) gaps.first() else emptyList()
        return anchors.mapIndexed { index, anchor ->
            val gap = gaps[index + 1]
            val belowCount = when {
                index == anchors.lastIndex || alignment == CellAlignment.TOP -> gap.size
                else -> minOf(gap.size, above.size)
            }
            val text = (above + description(anchor) + gap.take(belowCount)).filter { it.isNotBlank() }
            above = gap.drop(belowCount)
            TableRow(anchor, text.joinToString(" "))
        }
    }
}
