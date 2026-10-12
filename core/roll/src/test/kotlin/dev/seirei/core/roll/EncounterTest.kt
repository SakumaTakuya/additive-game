package dev.seirei.core.roll

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterTest {
    @Test fun `POC and DEMO have mean six encounters over ten thousand seeds`() {
        listOf(RollConstants.POC, RollConstants.DEMO).forEach { constants ->
            val mean = (0 until 10_000)
                .sumOf { seed -> generateEncounters(seed.toLong(), constants).size }
                .toDouble() / 10_000

            assertEquals(6.0, mean, 0.1)
        }
    }

    @Test fun `encounter offsets are sorted and within expedition duration`() {
        listOf(RollConstants.POC, RollConstants.DEMO).forEach { constants ->
            val encounters = generateEncounters(0x5EED, constants)

            assertEquals(encounters.indices.toList(), encounters.map { it.index })
            assertTrue(encounters.zipWithNext().all { (left, right) ->
                left.atOffsetMin <= right.atOffsetMin
            })
            assertTrue(encounters.all { it.atOffsetMin in 0 until constants.expeditionMinutes })
            assertTrue(encounters.all { it.motifId in 1..10 })
        }
    }

    @Test fun `same seed produces the same encounter sequence`() {
        listOf(RollConstants.POC, RollConstants.DEMO).forEach { constants ->
            assertEquals(
                generateEncounters(0x5EED, constants),
                generateEncounters(0x5EED, constants),
            )
        }
    }
}
