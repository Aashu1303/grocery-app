package app.rasoi.server.parsing.extract

/** All text of an invoice with positions, page by page. Parsers never see the PDF itself. */
data class ExtractedDocument(val pages: List<ExtractedPage>) {
    val words: List<TextWord> get() = pages.flatMap { it.words }

    /** The plain text of every page joined, for signature matching. */
    val plainText: String get() = pages.joinToString("\n") { page -> page.words.joinToString(" ") { it.text } }
}
