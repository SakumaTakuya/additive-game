package dev.seirei.core.roll

import org.junit.Assert.assertEquals
import org.junit.Test

class StarDistributionTest {
    @Test fun `base and fully boosted star distributions match design`() {
        val base = stars(quellCount = 0)
        assertEquals(15.0, base.count { it >= 4 } * 100.0 / base.size, 0.5)
        assertEquals(3.0, base.count { it == 5 } * 100.0 / base.size, 0.3)

        val boosted = stars(quellCount = 5)
        assertEquals(10.0, boosted.count { it == 5 } * 100.0 / boosted.size, 0.5)
    }

    private fun stars(quellCount: Int): List<Int> = (0 until SAMPLE_COUNT).flatMap { seed ->
        rollAwakened(seed.toLong(), quellCount, false, RollConstants.POC).map { it.star }
    }

    private companion object {
        const val SAMPLE_COUNT = 100_000
    }
}
