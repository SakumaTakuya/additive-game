package dev.seirei.core.roll.guarantee

import dev.seirei.core.roll.RollConstants
import dev.seirei.core.roll.RollEngine
import dev.seirei.core.roll.generateEncounters
import dev.seirei.core.roll.neutralParty
import dev.seirei.core.roll.neutralSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

/** 決定論（設計書02 §2・§8）。エンジンは4引数だけで結果が決まる純関数であること。 */
class DeterminismTest {
    @Test fun `同じ4引数なら探索結果は同じ`() {
        listOf(0L, 1L, 42L, Long.MAX_VALUE).forEach { seed ->
            val first = RollEngine.resolve(seed, neutralSnapshot, neutralParty(3), RollConstants.POC)
            val second = RollEngine.resolve(seed, neutralSnapshot, neutralParty(3), RollConstants.POC)
            assertEquals(first, second)
        }
    }

    @Test fun `同じシードなら遭遇列も同じ`() {
        listOf(RollConstants.POC, RollConstants.DEMO).forEach { constants ->
            assertEquals(generateEncounters(0x5EED, constants), generateEncounters(0x5EED, constants))
        }
    }
}
