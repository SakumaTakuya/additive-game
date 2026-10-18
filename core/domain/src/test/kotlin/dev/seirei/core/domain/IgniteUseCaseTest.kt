package dev.seirei.core.domain

import dev.seirei.core.model.ContextSnapshot
import dev.seirei.core.model.ExplorationSession
import dev.seirei.core.model.SessionPhase
import dev.seirei.core.model.TimeBand
import dev.seirei.core.model.WeatherKind
import dev.seirei.core.model.derivePhase
import dev.seirei.core.roll.RollConstants
import dev.seirei.core.testing.FakeClock
import dev.seirei.core.testing.aSession
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class IgniteUseCaseTest {
    private val tokyo = ZoneId.of("Asia/Tokyo")

    @Test
    fun `idle session is ignited with poc duration and point in time snapshot`() = runTest {
        val now = Instant.parse("2026-10-16T15:00:00Z")
        val clock = FakeClock(now)
        val sessionStore = FakeSessionStore()
        val partyPresetStore = FakePartyPresetStore(listOf(11L, 22L))
        val weatherReader = FakeWeatherReader(WeatherKind.RAIN)
        val seedSource = FakeSeedSource(987654321L)

        val result = useCase(clock, sessionStore, partyPresetStore, weatherReader, seedSource).invoke()

        assertEquals(IgniteResult.IGNITED, result)
        val session = sessionStore.current
        requireNotNull(session)
        assertEquals(1L, session.id)
        assertEquals(now, session.startedAt)
        assertEquals(now.plus(Duration.ofMinutes(120)), session.endAt)
        assertEquals(987654321L, session.seed)
        assertEquals(listOf(11L, 22L), session.partyIds)
        assertEquals(
            ContextSnapshot(
                date = LocalDate.of(2026, 10, 17),
                timeBand = TimeBand.NIGHT,
                weather = WeatherKind.RAIN,
            ),
            session.snapshot,
        )
        assertEquals(1, sessionStore.writes.size)
    }

    @Test
    fun `idle session after a collected session increments id`() = runTest {
        val now = Instant.parse("2026-10-17T00:00:00Z")
        val previous = aSession(id = 41L, collected = true)
        val sessionStore = FakeSessionStore(previous)

        val result = useCase(
            clock = FakeClock(now),
            sessionStore = sessionStore,
            partyPresetStore = FakePartyPresetStore(emptyList()),
            weatherReader = FakeWeatherReader(WeatherKind.UNKNOWN),
            seedSource = FakeSeedSource(12L),
        ).invoke()

        assertEquals(IgniteResult.IGNITED, result)
        assertEquals(42L, sessionStore.current?.id)
    }

    @Test
    fun `exploring session does not write or read ignition inputs`() = runTest {
        val now = Instant.parse("2026-10-17T00:00:00Z")
        val exploring = aSession(
            startedAt = now.minusSeconds(60),
            endAt = now.plusSeconds(60),
            collected = false,
        )
        assertEquals(SessionPhase.EXPLORING, derivePhase(exploring, now))
        val sessionStore = FakeSessionStore(exploring)
        val partyPresetStore = FakePartyPresetStore(emptyList())
        val weatherReader = FakeWeatherReader(WeatherKind.SUNNY)
        val seedSource = FakeSeedSource(1L)

        val result = useCase(
            FakeClock(now), sessionStore, partyPresetStore, weatherReader, seedSource,
        ).invoke()

        assertEquals(IgniteResult.NO_OP, result)
        assertEquals(emptyList<ExplorationSession>(), sessionStore.writes)
        assertEquals(exploring, sessionStore.current)
        assertEquals(0, partyPresetStore.reads)
        assertEquals(0, weatherReader.reads)
        assertEquals(0, seedSource.reads)
    }

    @Test
    fun `returned session does not write or read ignition inputs`() = runTest {
        val now = Instant.parse("2026-10-17T00:00:00Z")
        val returned = aSession(endAt = now.minusNanos(1), collected = false)
        assertEquals(SessionPhase.RETURNED, derivePhase(returned, now))
        val sessionStore = FakeSessionStore(returned)
        val partyPresetStore = FakePartyPresetStore(emptyList())
        val weatherReader = FakeWeatherReader(WeatherKind.SUNNY)
        val seedSource = FakeSeedSource(1L)

        val result = useCase(
            FakeClock(now), sessionStore, partyPresetStore, weatherReader, seedSource,
        ).invoke()

        assertEquals(IgniteResult.NO_OP, result)
        assertEquals(emptyList<ExplorationSession>(), sessionStore.writes)
        assertEquals(returned, sessionStore.current)
        assertEquals(0, partyPresetStore.reads)
        assertEquals(0, weatherReader.reads)
        assertEquals(0, seedSource.reads)
    }

    @Test
    fun `concurrent idle ignitions only replace the current session once`() = runTest {
        val sessionStore = ConcurrentSessionStore()
        val partyPresetStore = FakePartyPresetStore(emptyList())
        val weatherReader = FakeWeatherReader(WeatherKind.UNKNOWN)
        val seedSource = FakeSeedSource(123L)
        val useCase = useCase(
            FakeClock(Instant.parse("2026-10-17T00:00:00Z")),
            sessionStore,
            partyPresetStore,
            weatherReader,
            seedSource,
        )

        val results = listOf(
            async { useCase.invoke() },
            async { useCase.invoke() },
        ).awaitAll()

        assertEquals(1, results.count { it == IgniteResult.IGNITED })
        assertEquals(1, results.count { it == IgniteResult.NO_OP })
        assertEquals(1, sessionStore.writes.size)
    }

    @Test
    fun `unknown weather still allows ignition`() = runTest {
        val sessionStore = FakeSessionStore()

        val result = useCase(
            FakeClock(Instant.parse("2026-10-17T00:00:00Z")),
            sessionStore,
            FakePartyPresetStore(emptyList()),
            FakeWeatherReader(WeatherKind.UNKNOWN),
            FakeSeedSource(2L),
        ).invoke()

        assertEquals(IgniteResult.IGNITED, result)
        assertEquals(WeatherKind.UNKNOWN, sessionStore.current?.snapshot?.weather)
    }

    @Test
    fun `time band is day at 16 59 in tokyo`() = runTest {
        val session = igniteAt("2026-10-17T07:59:00Z")

        assertEquals(TimeBand.DAY, session.snapshot.timeBand)
    }

    @Test
    fun `time band is evening at 17 00 in tokyo`() = runTest {
        val session = igniteAt("2026-10-17T08:00:00Z")

        assertEquals(TimeBand.EVENING, session.snapshot.timeBand)
    }

    private suspend fun igniteAt(instant: String): ExplorationSession {
        val sessionStore = FakeSessionStore()
        val result = useCase(
            FakeClock(Instant.parse(instant)),
            sessionStore,
            FakePartyPresetStore(emptyList()),
            FakeWeatherReader(WeatherKind.UNKNOWN),
            FakeSeedSource(1L),
        ).invoke()

        assertEquals(IgniteResult.IGNITED, result)
        return requireNotNull(sessionStore.current)
    }

    private fun useCase(
        clock: FakeClock,
        sessionStore: SessionStore,
        partyPresetStore: FakePartyPresetStore,
        weatherReader: FakeWeatherReader,
        seedSource: FakeSeedSource,
    ) = IgniteUseCase(
        clock = clock,
        zone = tokyo,
        constants = RollConstants.POC,
        sessionStore = sessionStore,
        partyPresetStore = partyPresetStore,
        weatherReader = weatherReader,
        seedSource = seedSource,
    )

    private class FakeSessionStore(initial: ExplorationSession? = null) : SessionStore {
        var current: ExplorationSession? = initial
            private set
        val writes = mutableListOf<ExplorationSession>()

        override suspend fun loadCurrent(): ExplorationSession? = current

        override suspend fun replaceCurrentIfUnchanged(
            expectedCurrent: ExplorationSession?,
            replacement: ExplorationSession,
        ): Boolean {
            if (current != expectedCurrent) return false
            writes += replacement
            current = replacement
            return true
        }
    }

    private class ConcurrentSessionStore : SessionStore {
        private var current: ExplorationSession? = null
        private var loadCount = 0
        private val bothLoaded = CompletableDeferred<Unit>()
        val writes = mutableListOf<ExplorationSession>()

        override suspend fun loadCurrent(): ExplorationSession? {
            val snapshot = synchronized(this) {
                loadCount += 1
                if (loadCount == 2) bothLoaded.complete(Unit)
                current
            }
            bothLoaded.await()
            return snapshot
        }

        override suspend fun replaceCurrentIfUnchanged(
            expectedCurrent: ExplorationSession?,
            replacement: ExplorationSession,
        ): Boolean = synchronized(this) {
            if (current != expectedCurrent) return@synchronized false
            writes += replacement
            current = replacement
            true
        }
    }

    private class FakePartyPresetStore(private val partyIds: List<Long>) : PartyPresetStore {
        var reads: Int = 0
            private set

        override suspend fun loadPreviousPartyIds(): List<Long> {
            reads += 1
            return partyIds
        }
    }

    private class FakeWeatherReader(private val weather: WeatherKind) : WeatherReader {
        var reads: Int = 0
            private set

        override suspend fun effectiveWeather(now: Instant): WeatherKind {
            reads += 1
            return weather
        }
    }

    private class FakeSeedSource(private val seed: Long) : SeedSource {
        var reads: Int = 0
            private set

        override fun nextSeed(): Long {
            reads += 1
            return seed
        }
    }
}
