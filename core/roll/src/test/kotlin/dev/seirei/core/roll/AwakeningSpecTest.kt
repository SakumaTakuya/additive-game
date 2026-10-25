package dev.seirei.core.roll

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ③ 覚醒数と ④ 個体ロール（設計書02 §4・§5.4）。
 *
 * 覚醒は1体確定で、鎮め数に応じて2体目の上振れがある（PoC は鎮め3回で25%、4回で40%、5回以上で50%。DEC-02 の暫定値）。
 * ★の分布は鎮め数に応じて base から boosted へ線形に寄る（t = min(1, 鎮め数/5)）。
 * 分布の実測は guarantee/StatisticsTest、値域の不変条件は guarantee/InvariantTest。
 */
class AwakeningSpecTest {
    private val constants = RollConstants.POC

    // ---- 代表例 ----

    @Test fun `鎮めが0回なら覚醒はちょうど1体`() {
        (0L until 100L).forEach { seed ->
            assertEquals("seed=$seed", 1, rollAwakened(seed, quellCount = 0, earlyReturn = false, constants = constants).size)
        }
    }

    // ---- バリエーション ----

    @Test fun `鎮めが多いと2体目が覚醒することがあるが、3体以上にはならない`() {
        val counts = (0L until 200L).map { seed ->
            rollAwakened(seed, quellCount = 5, earlyReturn = false, constants = constants).size
        }

        assertTrue(counts.any { it == 2 })
        assertTrue(counts.all { it in 1..2 })
    }

    @Test fun `早期帰還なら鎮め数によらず1体だけ`() {
        (0L until 100L).forEach { seed ->
            assertEquals("seed=$seed", 1, rollAwakened(seed, quellCount = 5, earlyReturn = true, constants = constants).size)
        }
    }

    // ---- 境界 ----

    @Test fun `上振れ表より多く鎮めても表の最後の値を使う`() {
        val atTableEnd = (0L until 200L).map { rollAwakened(it, quellCount = 5, earlyReturn = false, constants = constants) }
        val beyondTable = (0L until 200L).map { rollAwakened(it, quellCount = 50, earlyReturn = false, constants = constants) }

        assertEquals(atTableEnd.map { it.size }, beyondTable.map { it.size })
    }

    // ---- 異常系 ----

    @Test fun `鎮め数が負なら例外`() {
        assertThrows(IllegalArgumentException::class.java) {
            rollAwakened(seed = 1, quellCount = -1, earlyReturn = false, constants = constants)
        }
    }
}
