package dev.seirei.core.roll

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistributionsTest {
    @Test fun `poisson lambda 6 has expected mean and variance`() {
        val rng = Rng(0x5EED)
        val samples = IntArray(100_000) { rng.poisson(6.0) }
        val mean = samples.average()
        val variance = samples.sumOf { (it - mean) * (it - mean) } / samples.size

        assertEquals(6.0, mean, 0.05)
        assertEquals(6.0, variance, 0.15)
    }

    @Test fun `poisson remains correct when exp negative lambda underflows`() {
        val rng = Rng(0x745)
        val samples = IntArray(1_000) { rng.poisson(800.0) }

        assertEquals(800.0, samples.average(), 4.0)
    }

    @Test fun `weightedIndex follows the requested proportions`() {
        val rng = Rng(0x51A7)
        val weights = doubleArrayOf(0.85, 0.12, 0.03)
        val counts = IntArray(weights.size)
        repeat(100_000) { counts[rng.weightedIndex(weights)]++ }

        weights.forEachIndexed { index, weight ->
            assertEquals(weight, counts[index].toDouble() / 100_000, 0.005)
        }
    }

    @Test fun `distribution helpers are deterministic for the same seed`() {
        val first = Rng(1234)
        val second = Rng(1234)
        repeat(1_000) {
            assertEquals(first.poisson(6.0), second.poisson(6.0))
            assertEquals(first.weightedIndex(doubleArrayOf(0.85, 0.12, 0.03)),
                second.weightedIndex(doubleArrayOf(0.85, 0.12, 0.03)))
        }
    }

    @Test fun `poisson zero lambda is always zero`() {
        val rng = Rng(7)
        repeat(10) { assertEquals(0, rng.poisson(0.0)) }
    }

    @Test fun `weightedIndex never selects zero-weight entries`() {
        val rng = Rng(9)
        repeat(1_000) { assertTrue(rng.weightedIndex(doubleArrayOf(0.0, 2.0, 0.0)) == 1) }
    }
}
