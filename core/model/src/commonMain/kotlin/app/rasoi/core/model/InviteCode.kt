package app.rasoi.core.model

import kotlin.jvm.JvmInline

@JvmInline
value class InviteCode(val value: String) {
    init {
        require(value.isNotBlank()) { "InviteCode must not be blank" }
    }
}
