package app.rasoi.server.parsing.layout

import app.rasoi.server.parsing.extract.TextWord

/** Words that share a baseline on one page, left to right. */
data class Line(val page: Int, val y: Float, val words: List<TextWord>) {
    val text: String get() = words.joinToString(" ") { it.text }

    fun wordsIn(range: ClosedFloatingPointRange<Float>): List<TextWord> = words.filter { it.x in range }

    fun contains(phrase: String): Boolean = text.contains(phrase, ignoreCase = true)
}
