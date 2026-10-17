package dev.seirei.core.roll

import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewMatchTest {
    @Test fun `MinimalBattleResolver uses the same arrowsFor values shown in preview`() {
        val snapshot = weekendSnapshot
        val party = cappedParty
        val previewQuellArrows = party.members.indices.map { slot ->
            arrowsFor(slot, party, snapshot).quell
        }
        val resolver = MinimalBattleResolver(RollConstants.POC)

        assertEquals(previewQuellArrows, resolver.quellArrowsFor(party, snapshot))
        assertEquals(
            (0 until party.members.size).sumOf { RollConstants.POC.memberBase +
                RollConstants.POC.starPt * (party.members[it].star - 1) +
                RollConstants.POC.arrowPt * previewQuellArrows[it] }.coerceAtMost(RollConstants.POC.pMax),
            resolver.winRate(party, snapshot),
            1e-12,
        )
    }
}
