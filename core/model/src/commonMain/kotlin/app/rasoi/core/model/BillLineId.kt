package app.rasoi.core.model

import kotlin.jvm.JvmInline

@JvmInline
value class BillLineId(val value: String) {
    init {
        require(value.isNotBlank()) { "BillLineId must not be blank" }
    }
}
