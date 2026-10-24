package dev.seirei.core.roll.guarantee

import dev.seirei.core.roll.Rng
import dev.seirei.core.roll.poisson
import dev.seirei.core.roll.weightedIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 乱数から分布を作る補助関数（ポアソン・重み付き選択）の正しさ。遭遇数と★帯の抽選がこれに乗っている。 */
class DistributionsTest {

    // ---- 境界 ----

    @Test fun `ポアソンはλ=0なら常に0`() {
        val rng = Rng(7)
        repeat(10) { assertEquals(0, rng.poisson(0.0)) }
    }

    @Test fun `重み0の項目は選ばれない`() {
        val rng = Rng(9)
        repeat(1_000) { assertTrue(rng.weightedIndex(doubleArrayOf(0.0, 2.0, 0.0)) == 1) }
    }

    @Test fun `ポアソンはexp(−λ)がアンダーフローする大きなλでも平均がλになる`() {
        val rng = Rng(0x745)
        val samples = IntArray(1_000) { rng.poisson(800.0) }

        assertEquals(800.0, samples.average(), 4.0)
    }

    // ---- 決定論 ----

    @Test fun `同じシードなら同じ値列`() {
        val first = Rng(1234)
        val second = Rng(1234)
        repeat(1_000) {
            assertEquals(first.poisson(6.0), second.poisson(6.0))
            assertEquals(
                first.weightedIndex(doubleArrayOf(0.85, 0.12, 0.03)),
                second.weightedIndex(doubleArrayOf(0.85, 0.12, 0.03)),
            )
        }
    }

    // ---- 統計（10万サンプル） ----

    @Test fun `ポアソンλ=6の平均と分散はどちらも6`() {
        val rng = Rng(0x5EED)
        val samples = IntArray(100_000) { rng.poisson(6.0) }
        val mean = samples.average()
        val variance = samples.sumOf { (it - mean) * (it - mean) } / samples.size

        assertEquals(6.0, mean, 0.05)
        assertEquals(6.0, variance, 0.15)
    }

    @Test fun `重み付き選択は重みの比率どおりに選ぶ`() {
        val rng = Rng(0x51A7)
        val weights = doubleArrayOf(0.85, 0.12, 0.03)
        val counts = IntArray(weights.size)
        repeat(100_000) { counts[rng.weightedIndex(weights)]++ }

        weights.forEachIndexed { index, weight ->
            assertEquals(weight, counts[index].toDouble() / 100_000, 0.005)
        }
    }
}
