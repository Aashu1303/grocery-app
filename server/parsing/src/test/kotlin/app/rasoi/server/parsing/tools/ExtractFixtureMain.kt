package app.rasoi.server.parsing.tools

import app.rasoi.core.model.AppResult
import app.rasoi.server.parsing.extract.ExtractedDocument
import app.rasoi.server.parsing.extract.ExtractedDocumentCodec
import app.rasoi.server.parsing.extract.PdfTextExtractor
import app.rasoi.server.parsing.extract.TextWord
import java.io.File
import kotlin.system.exitProcess

/**
 * Turns a local (git-ignored) invoice PDF into a committed, redacted fixture:
 * `./gradlew :server:parsing:extractFixture --args="in.pdf out.redacted.txt 'Real Name=>CUSTOMER NAME' ..."`.
 * Each replacement swaps an exact word (or a run of words separated by spaces) and the tool refuses to write a
 * fixture that still contains any replaced value. The replacement pairs are PII: pass them on the command line only.
 */
fun main(args: Array<String>) {
    if (args.size < 2) {
        System.err.println("usage: extractFixture <invoice.pdf> <out.redacted.txt> ['FROM=>TO' ...]")
        exitProcess(2)
    }
    val replacements = args.drop(2).map { rule ->
        val (from, to) = rule.split("=>", limit = 2).also { require(it.size == 2) { "Bad rule: $rule" } }
        from to to
    }
    val extracted = when (val result = PdfTextExtractor().extract(File(args[0]).readBytes())) {
        is AppResult.Success -> result.value
        is AppResult.Failure -> {
            System.err.println("Cannot extract: ${result.error}")
            exitProcess(1)
        }
    }
    val document = Redactor(replacements).redact(extracted)
    val leaks = Redactor.leaks(document, replacements.map { it.first })
    if (leaks.isNotEmpty()) {
        System.err.println("Refusing to write: redacted values still present: ${leaks.size}")
        exitProcess(1)
    }
    File(args[1]).writeText(ExtractedDocumentCodec.encode(document))
    println("Wrote ${document.words.size} words on ${document.pages.size} page(s) to ${args[1]}")
}

/** Replaces exact words, or runs of consecutive words on one line, with placeholders. */
class Redactor(private val replacements: List<Pair<String, String>>) {
    fun redact(document: ExtractedDocument): ExtractedDocument = document.copy(
        pages = document.pages.map { page ->
            var words = page.words
            replacements.forEach { (from, to) -> words = replaceRun(words, from.split(' '), to) }
            page.copy(words = words)
        },
    )

    private fun replaceRun(words: List<TextWord>, run: List<String>, to: String): List<TextWord> {
        val result = mutableListOf<TextWord>()
        var index = 0
        while (index < words.size) {
            val candidate = words.subList(index, minOf(words.size, index + run.size))
            if (candidate.map { it.text } == run) {
                val first = candidate.first()
                result += first.copy(text = to.replace(' ', '_'), width = candidate.last().right - first.x)
                index += run.size
            } else {
                result += words[index]
                index++
            }
        }
        return result
    }

    companion object {
        fun leaks(document: ExtractedDocument, values: List<String>): List<String> {
            val text = document.plainText.lowercase()
            return values.filter { text.contains(it.lowercase()) }
        }
    }
}
