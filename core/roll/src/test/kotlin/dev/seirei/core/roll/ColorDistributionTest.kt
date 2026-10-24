package dev.seirei.core.roll

import dev.seirei.core.model.ColorTier
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorDistributionTest {
    @Test fun `resident colors match normal rare and glow targets`() {
        assertRates(RollConstants.POC, normal = 91.0, rare = 8.0, glow = 1.0)
        assertRates(RollConstants.DEMO, normal = 75.0, rare = 15.0, glow = 10.0)
    }

    private fun assertRates(
        constants: RollConstants,
        normal: Double,
        rare: Double,
        glow: Double,
    ) {
        val colors = (0 until SAMPLE_COUNT).flatMap { seed ->
            rollAwakened(seed.toLong(), 0, false, constants).map { it.color }
        }
        assertEquals(normal, colors.count { it == ColorTier.NORMAL } * 100.0 / colors.size, 0.5)
        assertEquals(rare, colors.count { it == ColorTier.RARE } * 100.0 / colors.size, 0.5)
        assertEquals(glow, colors.count { it == ColorTier.GLOW } * 100.0 / colors.size, 0.5)
    }

    private companion object {
        const val SAMPLE_COUNT = 100_000
    }
}
