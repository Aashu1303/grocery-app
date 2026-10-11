package app.rasoi.core.model

import kotlin.jvm.JvmInline

@JvmInline
value class BillId(val value: String) {
    init {
        require(value.isNotBlank()) { "BillId must not be blank" }
    }
}
