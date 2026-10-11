package app.rasoi.core.model

import kotlin.jvm.JvmInline

@JvmInline
value class UserId(val value: String) {
    init {
        require(value.isNotBlank()) { "UserId must not be blank" }
    }
}
