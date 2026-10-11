package app.rasoi.server.parsing.fallback

import app.rasoi.server.parsing.extract.ExtractedDocument
import app.rasoi.server.parsing.extract.ExtractedPage
import app.rasoi.server.parsing.extract.TextWord
import app.rasoi.server.parsing.layout.Layout
import app.rasoi.server.parsing.layout.Line

/** A one-page document with one text line per entry, words spaced left to right. */
fun documentOf(vararg lines: String) = ExtractedDocument(
    listOf(
        ExtractedPage(
            1,
            595f,
            842f,
            lines.flatMapIndexed { row, text ->
                text.split(' ').filter { it.isNotEmpty() }.mapIndexed { column, word ->
                    TextWord(word, 20f + column * 60f, 20f + row * 12f, 50f, 8f)
                }
            },
        ),
    ),
)

fun linesOf(vararg lines: String): List<Line> = Layout(documentOf(*lines)).lines
