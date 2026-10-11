package app.rasoi.server.parsing.layout

import app.rasoi.server.parsing.extract.ExtractedDocument
import app.rasoi.server.parsing.extract.TextWord

/** Groups a document's words into lines: same page, centres within half a glyph height. */
class Layout(document: ExtractedDocument) {
    val lines: List<Line> = document.pages.flatMap { page ->
        val sorted = page.words.sortedWith(compareBy({ it.centerY }, { it.x }))
        val groups = mutableListOf<MutableList<TextWord>>()
        sorted.forEach { word ->
            val current = groups.lastOrNull()
            val tolerance = word.height * LINE_TOLERANCE
            if (current != null && kotlin.math.abs(current.first().centerY - word.centerY) <= tolerance) {
                current += word
            } else {
                groups += mutableListOf(word)
            }
        }
        groups.map { words -> Line(page.number, words.minOf { it.y }, words.sortedBy { it.x }) }
    }

    /** The first line containing [phrase], case-insensitively. */
    fun find(phrase: String): Line? = lines.firstOrNull { it.contains(phrase) }

    /** The text of the word that follows [label] on the same line, skipping a lone colon. */
    fun valueAfter(label: String): String? {
        val labelWords = label.split(' ')
        lines.forEach { line ->
            val texts = line.words.map { it.text }
            val start = texts.windowed(labelWords.size).indexOfFirst { window ->
                window.zip(labelWords).all { (actual, expected) -> actual.equals(expected, ignoreCase = true) }
            }
            if (start >= 0) {
                return texts.drop(start + labelWords.size).firstOrNull { it != ":" }
            }
        }
        return null
    }

    private companion object {
        const val LINE_TOLERANCE = 0.5f
    }
}
