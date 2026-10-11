package app.rasoi.core.testing

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/** A clock that only moves when a test says so. */
class FixedClock(var current: Instant = Instant.parse("2026-10-11T10:00:00Z")) : Clock {
    override fun now(): Instant = current

    fun advance(by: Duration) {
        current += by
    }
}
