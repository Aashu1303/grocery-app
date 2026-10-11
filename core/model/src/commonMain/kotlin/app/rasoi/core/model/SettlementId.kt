package app.rasoi.core.model

import kotlin.jvm.JvmInline

@JvmInline
value class SettlementId(val value: String) {
    init {
        require(value.isNotBlank()) { "SettlementId must not be blank" }
    }
}
