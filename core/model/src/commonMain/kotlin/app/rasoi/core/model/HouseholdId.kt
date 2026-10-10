package app.rasoi.core.model

import kotlin.jvm.JvmInline

@JvmInline
value class HouseholdId(val value: String) {
    init {
        require(value.isNotBlank()) { "HouseholdId must not be blank" }
    }
}
