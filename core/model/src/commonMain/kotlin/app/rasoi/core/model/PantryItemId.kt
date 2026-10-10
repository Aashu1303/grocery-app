package app.rasoi.core.model

import kotlin.jvm.JvmInline

@JvmInline
value class PantryItemId(val value: String) {
    init {
        require(value.isNotBlank()) { "PantryItemId must not be blank" }
    }
}
