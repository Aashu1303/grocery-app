package app.rasoi.core.model

import kotlin.jvm.JvmInline

@JvmInline
value class PeriodId(val value: String) {
    init {
        require(value.isNotBlank()) { "PeriodId must not be blank" }
    }
}
