package dev.seirei.core.domain

import dev.seirei.core.model.ColorTier
import dev.seirei.core.model.ExplorationSession
import dev.seirei.core.model.Party
import dev.seirei.core.model.PartyMember
import dev.seirei.core.model.Resident
import dev.seirei.core.model.SessionPhase
import dev.seirei.core.model.derivePhase
import dev.seirei.core.roll.ExpeditionResult
import dev.seirei.core.roll.RollConstants
import dev.seirei.core.roll.RollEngine
import dev.seirei.core.testing.FakeClock
import dev.seirei.core.testing.aSession
import java.time.Instant
import java.time.Duration
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectUseCaseTest {
    @Test
    fun `returned session is rolled and written once`() = runTest {
        val now = Instant.parse("2026-10-17T02:00:00Z")
        val session = aSession(endAt = now, partyIds = listOf(22L, 11L))
        assertEquals(SessionPhase.RETURNED, derivePhase(session, now))
        val clock = FakeClock(now)
        val sessionStore = FakeSessionStore(session)
        val residents = FakeResidentStore(listOf(resident(11L), resident(22L, motifId = 2)))
        val collectStore = FakeCollectStore(sessionStore)

        val outcome = useCase(clock, sessionStore, residents, collectStore).invoke()

        assertTrue(outcome is CollectOutcome.Collected)
        assertEquals(1, collectStore.writes.size)
        assertEquals(session, collectStore.writes.single().first)
        assertEquals(listOf(22L, 11L), residents.requestedIds.single())
        val expected = RollEngine.resolve(
            session.seed,
            session.snapshot,
            Party(
                listOf(
                    PartyMember(2, 2, 0),
                    PartyMember(1, 2, 0),
                ),
            ),
            RollConstants.POC,
        )
        assertEquals(CollectOutcome.Collected(expected), outcome)
    }

    @Test
    fun `repeated collect after success does not write again`() = runTest {
        val now = Instant.parse("2026-10-17T02:00:00Z")
        val sessionStore = FakeSessionStore(aSession(endAt = now))
        val collectStore = FakeCollectStore(sessionStore)
        val useCase = useCase(FakeClock(now), sessionStore, FakeResidentStore(emptyList()), collectStore)

        assertTrue(useCase.invoke() is CollectOutcome.Collected)
        assertEquals(CollectOutcome.NoOp, useCase.invoke())

        assertEquals(1, collectStore.writes.size)
    }

    @Test
    fun `old notification after collection is a no op`() = runTest {
        val now = Instant.parse("2026-10-17T02:00:00Z")
        val sessionStore = FakeSessionStore(aSession(endAt = now))
        val collectStore = FakeCollectStore(sessionStore)
        val useCase = useCase(FakeClock(now), sessionStore, FakeResidentStore(emptyList()), collectStore)
        useCase.invoke()

        assertEquals(CollectOutcome.NoOp, useCase.invoke())
        assertEquals(1, collectStore.writes.size)
    }

    @Test
    fun `clock rollback after returned refresh prevents collection`() = runTest {
        val endAt = Instant.parse("2026-10-17T02:00:00Z")
        val clock = FakeClock(endAt.plusSeconds(1))
        val session = aSession(endAt = endAt)
        val sessionStore = FakeSessionStore(session)
        val residentStore = FakeResidentStore(emptyList())
        val collectStore = FakeCollectStore(sessionStore)
        val useCase = useCase(clock, sessionStore, residentStore, collectStore)

        assertEquals(SessionPhase.RETURNED, derivePhase(sessionStore.loadCurrent(), clock.now()))
        clock.rewind(Duration.ofSeconds(2))
        assertEquals(CollectOutcome.NoOp, useCase.invoke())

        assertTrue(collectStore.writes.isEmpty())
        assertTrue(residentStore.requestedIds.isEmpty())
    }

    @Test
    fun `refresh reads before end do not write`() = runTest {
        val now = Instant.parse("2026-10-17T01:00:00Z")
        val sessionStore = FakeSessionStore(aSession(endAt = now.plusSeconds(3600)))
        val collectStore = FakeCollectStore(sessionStore)
        val useCase = useCase(FakeClock(now), sessionStore, FakeResidentStore(emptyList()), collectStore)

        repeat(5) { assertEquals(CollectOutcome.NoOp, useCase.invoke()) }

        assertTrue(collectStore.writes.isEmpty())
    }

    @Test
    fun `store duplicate receipt is returned as no op`() = runTest {
        val now = Instant.parse("2026-10-17T02:00:00Z")
        val session = aSession(endAt = now)
        val sessionStore = FakeSessionStore(session)
        val duplicateStore = CollectStore { _, _ -> CollectWriteResult.ALREADY_COLLECTED }

        assertEquals(
            CollectOutcome.NoOp,
            useCase(FakeClock(now), sessionStore, FakeResidentStore(emptyList()), duplicateStore).invoke(),
        )
    }

    private fun useCase(
        clock: FakeClock,
        sessionStore: FakeSessionStore,
        residentStore: FakeResidentStore,
        collectStore: CollectStore,
    ) = CollectUseCase(clock, RollConstants.POC, sessionStore, residentStore, collectStore)

    private class FakeSessionStore(var current: ExplorationSession?) : SessionStore {
        override suspend fun loadCurrent(): ExplorationSession? = current

        override suspend fun replaceCurrentIfUnchanged(
            expectedCurrent: ExplorationSession?,
            replacement: ExplorationSession,
        ): Boolean = error("Ignite is outside this test")
    }

    private class FakeResidentStore(private val residents: List<Resident>) : CollectResidentStore {
        val requestedIds = mutableListOf<List<Long>>()

        override suspend fun loadByIds(ids: List<Long>): List<Resident> {
            requestedIds += ids
            return residents.filter { it.id in ids }
        }
    }

    private class FakeCollectStore(private val sessionStore: FakeSessionStore) : CollectStore {
        val writes = mutableListOf<Pair<ExplorationSession, ExpeditionResult>>()

        override suspend fun collect(
            session: ExplorationSession,
            result: ExpeditionResult,
        ): CollectWriteResult {
            if (sessionStore.current?.id != session.id || sessionStore.current?.collected == true) {
                return CollectWriteResult.ALREADY_COLLECTED
            }
            writes += session to result
            sessionStore.current = session.copy(collected = true)
            return CollectWriteResult.COLLECTED
        }
    }

    private fun resident(id: Long, motifId: Int = 1) = Resident(
        id = id,
        motifId = motifId,
        star = 2,
        personalityId = 0,
        colorTier = ColorTier.NORMAL,
        acquiredAt = Instant.parse("2026-01-01T00:00:00Z"),
        sourceSessionId = 0L,
        locked = false,
    )
}
