package dev.seirei.core.roll

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RollConstantsTest {
    @Test
    fun `POC and DEMO star weights and low star breakdowns sum to one`() {
        listOf(RollConstants.POC, RollConstants.DEMO).forEach { constants ->
            assertEquals(1.0, constants.starBase.sum(), 1e-12)
            assertEquals(1.0, constants.starBoosted.sum(), 1e-12)
            assertEquals(1.0, constants.star1To3.sum(), 1e-12)
        }
    }

    @Test
    fun `all probability values are between zero and one`() {
        listOf(RollConstants.POC, RollConstants.DEMO).forEach { constants ->
            val probabilities = listOf(
                constants.starBase.first,
                constants.starBase.second,
                constants.starBase.third,
                constants.starBoosted.first,
                constants.starBoosted.second,
                constants.starBoosted.third,
                constants.star1To3.first,
                constants.star1To3.second,
                constants.star1To3.third,
                constants.rareColorRate,
                constants.glowColorRate,
                constants.manaArrowRate,
                constants.memberBase,
                constants.starPt,
                constants.arrowPt,
                constants.pMax,
            ) + constants.awakenUpsideTable

            probabilities.forEach { probability ->
                assertTrue("$probability should be in 0..1", probability in 0.0..1.0)
            }
        }
    }

    @Test
    fun `POC values match roll simulation inputs`() {
        val poc = RollConstants.POC

        assertEquals(120, poc.expeditionMinutes)
        assertEquals(3.0, poc.encounterRatePerHour, 0.0)
        assertEquals(0.85, poc.starBase.first, 0.0)
        assertEquals(0.12, poc.starBase.second, 0.0)
        assertEquals(0.03, poc.starBase.third, 0.0)
        assertEquals(0.70, poc.starBoosted.first, 0.0)
        assertEquals(0.20, poc.starBoosted.second, 0.0)
        assertEquals(0.10, poc.starBoosted.third, 0.0)
        assertEquals(5, poc.quellNorm)
        assertEquals(0.08, poc.rareColorRate, 0.0)
        assertEquals(0.01, poc.glowColorRate, 0.0)
        assertEquals(0.12, poc.memberBase, 0.0)
        assertEquals(0.02, poc.starPt, 0.0)
        assertEquals(0.05, poc.arrowPt, 0.0)
        assertEquals(0.90, poc.pMax, 0.0)
        assertEquals(0.10, poc.manaArrowRate, 0.0)
        assertEquals(12, poc.personalityCount)
    }
}
