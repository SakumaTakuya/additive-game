package dev.seirei.core.roll

import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveDeterminismTest {
    @Test fun `same four inputs produce equal expedition results`() {
        val inputs = listOf(0L, 1L, 42L, Long.MAX_VALUE)
        inputs.forEach { seed ->
            val first = RollEngine.resolve(seed, noModifierSnapshot, noModifierParty(3), RollConstants.POC)
            val second = RollEngine.resolve(seed, noModifierSnapshot, noModifierParty(3), RollConstants.POC)
            assertEquals(first, second)
        }
    }
}
