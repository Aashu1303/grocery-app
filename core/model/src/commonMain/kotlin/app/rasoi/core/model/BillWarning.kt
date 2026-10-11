package app.rasoi.core.model

/** Something the review screen shows before a bill can be submitted (design-plan 6.5, step 7). */
sealed interface BillWarning {
    /** Lines plus charges differ from the printed total. */
    data class TotalMismatch(val computed: Money, val printed: Money) : BillWarning

    /** The LLM fallback read this bill, so every line needs a check. */
    data object ParsedByFallback : BillWarning
}
