package dev.seirei.core.roll

import org.junit.Assert.assertTrue
import org.junit.Test

class AwakenInvariantTest {
    @Test fun `ten thousand seeds always produce valid resident rolls`() {
        repeat(10_000) { seed ->
            val residents = rollAwakened(seed.toLong(), seed % 6, false, RollConstants.POC)
            assertTrue("seed=$seed must awaken at least one", residents.isNotEmpty())
            residents.forEach { resident ->
                assertTrue("star=${resident.star}", resident.star in 1..5)
                assertTrue("motifId=${resident.motifId}", resident.motifId in 1..10)
                assertTrue("personalityId=${resident.personalityId}", resident.personalityId in 0..11)
            }
        }
    }
}
