package dev.seirei.core.domain

import dev.seirei.core.roll.ResidentRoll
import dev.seirei.core.roll.RollConstants
import dev.seirei.core.roll.resolveNamed
import dev.seirei.core.testing.FakeClock
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 指名覚醒（設計書03 §4.3・§7、DEC-03）。
 * 残高が CEILING_COST 以上で、モチーフが発見済みのときだけ resolveNamed して
 * 住人追加と魔力減算を行う。ガード不成立では何もしない。
 */
class NamedAwakenTest {
    private val now: Instant = Instant.parse("2026-10-25T00:00:00Z")
    private val seed = 4242L

    @Test
    fun `残高が足りれば400減り、指名したモチーフのロールが保存される`() = runTest {
        val store = FakeNamedAwakenStore(balance = 400, discovered = setOf(1, 2))

        val outcome = useCase(store).invoke(motifId = 2)

        val expectedRoll = resolveNamed(seed, 2, RollConstants.POC)
        assertEquals(NamedAwakenOutcome.Awakened(expectedRoll), outcome)
        assertEquals(0, store.balance)
        assertEquals(1, store.awakened.size)
        val (roll, acquiredAt, cost) = store.awakened.single()
        assertEquals(expectedRoll, roll)
        assertEquals(now, acquiredAt)
        assertEquals(CEILING_COST, cost)
    }

    @Test
    fun `残高が399では何もしない`() = runTest {
        val store = FakeNamedAwakenStore(balance = CEILING_COST - 1, discovered = setOf(1))

        assertEquals(NamedAwakenOutcome.NoOp, useCase(store).invoke(motifId = 1))

        assertEquals(CEILING_COST - 1, store.balance)
        assertTrue(store.awakened.isEmpty())
    }

    @Test
    fun `未発見のモチーフは残高があっても選べない`() = runTest {
        val store = FakeNamedAwakenStore(balance = 10_000, discovered = setOf(1))

        assertEquals(NamedAwakenOutcome.NoOp, useCase(store).invoke(motifId = 3))

        assertEquals(10_000, store.balance)
        assertTrue(store.awakened.isEmpty())
    }

    @Test
    fun `書き込み時点で残高不足になっていたら何もしない`() = runTest {
        val store = FakeNamedAwakenStore(balance = 400, discovered = setOf(1), failWrites = true)

        assertEquals(NamedAwakenOutcome.NoOp, useCase(store).invoke(motifId = 1))
        assertTrue(store.awakened.isEmpty())
    }

    private fun useCase(store: NamedAwakenStore): NamedAwakenUseCase = NamedAwakenUseCase(
        clock = FakeClock(now),
        constants = RollConstants.POC,
        store = store,
        seedSource = SeedSource { seed },
    )

    private class FakeNamedAwakenStore(
        balance: Int,
        private val discovered: Set<Int>,
        private val failWrites: Boolean = false,
    ) : NamedAwakenStore {
        var balance: Int = balance
            private set
        val awakened = mutableListOf<Triple<ResidentRoll, Instant, Int>>()

        override suspend fun loadBalance(): Int = balance

        override suspend fun loadDiscoveredMotifIds(): Set<Int> = discovered

        override suspend fun awaken(roll: ResidentRoll, acquiredAt: Instant, cost: Int): Boolean {
            if (failWrites || balance < cost) return false
            balance -= cost
            awakened += Triple(roll, acquiredAt, cost)
            return true
        }
    }
}
