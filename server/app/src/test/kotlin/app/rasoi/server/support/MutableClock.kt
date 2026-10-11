package app.rasoi.server.support

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

class MutableClock(var current: Instant = Instant.parse("2026-10-11T04:30:00Z")) : Clock {
    override fun now(): Instant = current

    fun advance(by: Duration) {
        current += by
    }
}
