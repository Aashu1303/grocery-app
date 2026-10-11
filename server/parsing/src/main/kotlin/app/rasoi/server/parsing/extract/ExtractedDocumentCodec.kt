package app.rasoi.server.parsing.extract

import java.util.Locale

/**
 * A line-based text form of [ExtractedDocument] used for committed, redacted test fixtures:
 * a `page <n> <width> <height>` line, then one `<x> <y> <width> <height> <text>` line per word.
 */
object ExtractedDocumentCodec {
    private const val HEADER = "# rasoi extracted document v1"

    fun encode(document: ExtractedDocument): String = buildString {
        appendLine(HEADER)
        document.pages.forEach { page ->
            appendLine("page ${page.number} ${page.width.fmt()} ${page.height.fmt()}")
            page.words.forEach {
                appendLine("${it.x.fmt()} ${it.y.fmt()} ${it.width.fmt()} ${it.height.fmt()} ${it.text}")
            }
        }
    }

    fun decode(text: String): ExtractedDocument {
        val pages = mutableListOf<ExtractedPage>()
        var words = mutableListOf<TextWord>()
        var header: List<String>? = null
        fun closePage() {
            header?.let { pages += ExtractedPage(it[1].toInt(), it[2].toFloat(), it[PAGE_HEIGHT].toFloat(), words) }
            words = mutableListOf()
        }
        text.lineSequence().filter { it.isNotBlank() && !it.startsWith("#") }.forEach { line ->
            if (line.startsWith("page ")) {
                closePage()
                header = line.split(' ')
            } else {
                val parts = line.split(' ', limit = FIELDS)
                require(parts.size == FIELDS) { "Malformed word line: $line" }
                val (x, y, width) = parts
                words += TextWord(parts.last(), x.toFloat(), y.toFloat(), width.toFloat(), parts[WORD_HEIGHT].toFloat())
            }
        }
        closePage()
        return ExtractedDocument(pages)
    }

    private fun Float.fmt(): String = String.format(Locale.ROOT, "%.2f", this)

    private const val FIELDS = 5
    private const val PAGE_HEIGHT = 3
    private const val WORD_HEIGHT = 3
}
