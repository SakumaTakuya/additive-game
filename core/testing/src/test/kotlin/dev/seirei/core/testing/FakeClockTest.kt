package dev.seirei.core.testing

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeClockTest {
    @Test
    fun `advance and rewind update now`() {
        val start = Instant.parse("2026-10-12T03:00:00Z")
        val clock = FakeClock(start)

        clock.advance(Duration.ofMinutes(5))
        assertEquals(start.plus(Duration.ofMinutes(5)), clock.now())

        clock.rewind(Duration.ofMinutes(8))
        assertEquals(start.minus(Duration.ofMinutes(3)), clock.now())
    }
}
