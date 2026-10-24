package dev.seirei.core.roll

import dev.seirei.core.model.ColorTier
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorDistributionTest {
    @Test fun `resident colors match normal rare and glow targets`() {
        val colors = (0 until SAMPLE_COUNT).flatMap { seed ->
            rollAwakened(seed.toLong(), 0, false, RollConstants.POC).map { it.color }
        }
        assertEquals(91.0, colors.count { it == ColorTier.NORMAL } * 100.0 / colors.size, 0.5)
        assertEquals(8.0, colors.count { it == ColorTier.RARE } * 100.0 / colors.size, 0.5)
        assertEquals(1.0, colors.count { it == ColorTier.GLOW } * 100.0 / colors.size, 0.5)
    }

    private companion object {
        const val SAMPLE_COUNT = 100_000
    }
}
