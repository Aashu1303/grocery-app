package app.rasoi.core.model

import kotlin.jvm.JvmInline

@JvmInline
value class LocationId(val value: String) {
    init {
        require(value.isNotBlank()) { "LocationId must not be blank" }
    }
}
