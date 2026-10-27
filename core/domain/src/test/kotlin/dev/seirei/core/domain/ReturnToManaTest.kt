package dev.seirei.core.domain

import dev.seirei.core.model.ColorTier
import dev.seirei.core.model.Resident
import dev.seirei.core.testing.aResident
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 魔力に還す（設計書03 §4.4・§7、DEC-03）。
 * ロック中と編成中は除外し、還元額 = 10 × 2^(★−1) × 色倍率（通常1・レア3・発光10）。
 * プレビューは書き込まずに合計だけを返す。
 */
class ReturnToManaTest {

    @Test
    fun `選んだ住人を還すと合計が魔力に加わり、住人が減る`() = runTest {
        val store = FakeReturnToManaStore(
            residents = listOf(
                aResident(id = 1L, star = 3, colorTier = ColorTier.RARE),
                aResident(id = 2L, star = 1, colorTier = ColorTier.NORMAL),
            ),
            partyIds = emptyList(),
        )

        val outcome = useCase(store).invoke(listOf(1L, 2L))

        assertEquals(ReturnToManaOutcome.Returned(refund = 130), outcome)
        assertEquals(130, store.manaAdded)
        assertTrue(store.remaining.isEmpty())
    }

    @Test
    fun `ロックされた住人と編成中の住人は除外される`() = runTest {
        val locked = aResident(id = 1L, star = 5, colorTier = ColorTier.GLOW, locked = true)
        val inParty = aResident(id = 2L, star = 5, colorTier = ColorTier.GLOW)
        val free = aResident(id = 3L, star = 2, colorTier = ColorTier.NORMAL)
        val store = FakeReturnToManaStore(
            residents = listOf(locked, inParty, free),
            partyIds = listOf(2L),
        )

        val outcome = useCase(store).invoke(listOf(1L, 2L, 3L))

        assertEquals(ReturnToManaOutcome.Returned(refund = 20), outcome)
        assertEquals(listOf(locked, inParty), store.remaining)
    }

    @Test
    fun `プレビューは書き込まずに対象分の合計だけを返す`() = runTest {
        val store = FakeReturnToManaStore(
            residents = listOf(
                aResident(id = 1L, star = 3, colorTier = ColorTier.RARE),
                aResident(id = 2L, star = 5, colorTier = ColorTier.GLOW, locked = true),
            ),
            partyIds = emptyList(),
        )

        assertEquals(120, useCase(store).preview(listOf(1L, 2L)))

        assertEquals(0, store.writeCalls)
        assertEquals(2, store.remaining.size)
    }

    @Test
    fun `対象がすべて除外されたら何もしない`() = runTest {
        val store = FakeReturnToManaStore(
            residents = listOf(aResident(id = 1L, locked = true)),
            partyIds = emptyList(),
        )

        assertEquals(ReturnToManaOutcome.NoOp, useCase(store).invoke(listOf(1L)))
        assertEquals(0, store.writeCalls)
        assertEquals(0, store.manaAdded)
    }

    @Test
    fun `書き込み時点でロック・編成入りした住人は還元から外れる`() = runTest {
        // プレビュー後に並行して状態が変わったケース：書き込み側が同じ
        // トランザクションで再チェックし、対象外になった分は還元しない。
        val store = FakeReturnToManaStore(
            residents = listOf(
                aResident(id = 1L, star = 1, colorTier = ColorTier.NORMAL),
                aResident(id = 2L, star = 1, colorTier = ColorTier.NORMAL),
                aResident(id = 3L, star = 1, colorTier = ColorTier.NORMAL),
            ),
            partyIds = emptyList(),
            lockOnWrite = setOf(2L),
            joinPartyOnWrite = setOf(3L),
        )

        val outcome = useCase(store).invoke(listOf(1L, 2L, 3L))

        assertEquals(ReturnToManaOutcome.Returned(refund = 10), outcome)
        assertEquals(10, store.manaAdded)
        assertEquals(listOf(2L, 3L), store.remaining.map(Resident::id))
    }

    @Test
    fun `還元額は星と色の表どおり`() {
        val cases = listOf(
            Triple(1, ColorTier.NORMAL, 10),
            Triple(2, ColorTier.NORMAL, 20),
            Triple(3, ColorTier.RARE, 120),
            Triple(4, ColorTier.NORMAL, 80),
            Triple(5, ColorTier.GLOW, 1600),
        )
        for ((star, color, expected) in cases) {
            assertEquals(expected, manaRefundFor(aResident(star = star, colorTier = color)))
        }
    }

    private fun useCase(store: ReturnToManaStore): ReturnToManaUseCase = ReturnToManaUseCase(store)

    private class FakeReturnToManaStore(
        residents: List<Resident>,
        private val partyIds: List<Long>,
        private val lockOnWrite: Set<Long> = emptySet(),
        private val joinPartyOnWrite: Set<Long> = emptySet(),
    ) : ReturnToManaStore {
        val remaining = residents.toMutableList()
        var manaAdded: Int = 0
            private set
        var writeCalls: Int = 0
            private set

        override suspend fun loadByIds(ids: List<Long>): List<Resident> =
            remaining.filter { it.id in ids }

        override suspend fun loadPartyIds(): List<Long> = partyIds

        override suspend fun returnToMana(targets: List<ReturnTarget>): Int? {
            writeCalls++
            var refunded = 0
            for (target in targets) {
                val current = remaining.firstOrNull { it.id == target.resident.id } ?: continue
                // Recheck eligibility as of the write, like the real transaction.
                if (current.locked || current.id in lockOnWrite ||
                    current.id in partyIds || current.id in joinPartyOnWrite
                ) {
                    continue
                }
                remaining.remove(current)
                refunded += target.refund
            }
            manaAdded += refunded
            return refunded
        }
    }
}
