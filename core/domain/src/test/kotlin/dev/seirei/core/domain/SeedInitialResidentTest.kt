package dev.seirei.core.domain

import dev.seirei.core.model.ExplorationSession
import dev.seirei.core.roll.ResidentRoll
import dev.seirei.core.roll.RollConstants
import dev.seirei.core.roll.resolveNamed
import dev.seirei.core.testing.FakeClock
import dev.seirei.core.testing.aSession
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 初期付与（設計書03 §4.6、DEC-11）。
 * 住人0体かつセッションなしのときだけ、モチーフ#1 を resolveNamed で1体付与する。冪等。
 */
class SeedInitialResidentTest {
    private val now: Instant = Instant.parse("2026-10-25T00:00:00Z")
    private val seed = 777L

    @Test
    fun `住人がおらずセッションもなければモチーフ1を1体付与する`() = runTest {
        val store = FakeSeedStore(count = 0)

        val outcome = useCase(session = null, store = store).invoke()

        val expectedRoll = resolveNamed(seed, 1, RollConstants.POC)
        assertEquals(SeedInitialOutcome.Seeded(expectedRoll), outcome)
        assertEquals(1, store.inserted.size)
        assertEquals(expectedRoll, store.inserted.single().first)
        assertEquals(now, store.inserted.single().second)
    }

    @Test
    fun `2回呼んでも1体だけ`() = runTest {
        val store = FakeSeedStore(count = 0)
        val useCase = useCase(session = null, store = store)

        assertTrue(useCase.invoke() is SeedInitialOutcome.Seeded)
        assertEquals(SeedInitialOutcome.NoOp, useCase.invoke())

        assertEquals(1, store.inserted.size)
        assertEquals(1, store.count)
    }

    @Test
    fun `住人がいれば付与しない`() = runTest {
        val store = FakeSeedStore(count = 1)

        assertEquals(SeedInitialOutcome.NoOp, useCase(session = null, store = store).invoke())
        assertTrue(store.inserted.isEmpty())
    }

    @Test
    fun `セッションがあれば付与しない`() = runTest {
        val store = FakeSeedStore(count = 0)

        assertEquals(SeedInitialOutcome.NoOp, useCase(session = aSession(), store = store).invoke())
        assertTrue(store.inserted.isEmpty())
    }

    @Test
    fun `回収済みセッションが残っていても付与しない`() = runTest {
        val store = FakeSeedStore(count = 0)

        val outcome = useCase(session = aSession(collected = true), store = store).invoke()

        assertEquals(SeedInitialOutcome.NoOp, outcome)
        assertTrue(store.inserted.isEmpty())
    }

    private fun useCase(
        session: ExplorationSession?,
        store: SeedInitialResidentStore,
    ): SeedInitialResidentUseCase = SeedInitialResidentUseCase(
        clock = FakeClock(now),
        constants = RollConstants.POC,
        sessionStore = FakeSessionStore(session),
        store = store,
        seedSource = SeedSource { seed },
    )

    private class FakeSessionStore(
        private val current: ExplorationSession?,
    ) : SessionStore {
        override suspend fun loadCurrent(): ExplorationSession? = current

        override suspend fun replaceCurrentIfUnchanged(
            expectedCurrent: ExplorationSession?,
            replacement: ExplorationSession,
        ): Boolean = false
    }

    private class FakeSeedStore(count: Int) : SeedInitialResidentStore {
        var count: Int = count
            private set
        val inserted = mutableListOf<Pair<ResidentRoll, Instant>>()

        override suspend fun countResidents(): Int = count

        override suspend fun insertInitial(roll: ResidentRoll, acquiredAt: Instant): Boolean {
            if (count > 0) return false
            count++
            inserted += roll to acquiredAt
            return true
        }
    }
}
