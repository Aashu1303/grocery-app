package app.rasoi.server.parsing.model

import app.rasoi.core.model.Money

/** Something the review screen must show before the bill can be submitted. */
sealed interface ParseWarning {
    /** Lines plus charges do not add up to the printed total. */
    data class TotalMismatch(val computed: Money, val printed: Money) : ParseWarning

    /** The LLM fallback produced the result, so every line needs a look. */
    data object ParsedByFallback : ParseWarning
}
