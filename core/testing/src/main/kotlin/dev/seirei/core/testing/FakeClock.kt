package dev.seirei.core.testing

import dev.seirei.core.model.Clock
import java.time.Duration
import java.time.Instant

/** A controllable [Clock] for deterministic tests. */
class FakeClock(startAt: Instant) : Clock {
    @Volatile
    private var current: Instant = startAt

    override fun now(): Instant = current

    fun advance(duration: Duration) {
        current = current.plus(duration)
    }

    fun rewind(duration: Duration) {
        current = current.minus(duration)
    }
}
