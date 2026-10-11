package app.rasoi.core.model

import kotlin.jvm.JvmInline

@JvmInline
value class ChargeId(val value: String) {
    init {
        require(value.isNotBlank()) { "ChargeId must not be blank" }
    }
}
