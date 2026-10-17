package dev.seirei.core.roll

import dev.seirei.core.model.Party
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WinRateTest {
    private val resolver = MinimalBattleResolver(RollConstants.POC)

    @Test fun `win rate matches design cases and clamps at pMax`() {
        assertEquals(0.00, resolver.winRate(Party(emptyList()), noModifierSnapshot), 1e-12)
        assertEquals(0.16, resolver.winRate(noModifierParty(3), noModifierSnapshot), 1e-12)
        assertEquals(0.46, resolver.winRate(noModifierParty(2, 3, 3), noModifierSnapshot), 1e-12)
        assertEquals(0.90, resolver.winRate(cappedParty, weekendSnapshot), 1e-12)
    }

    @Test fun `NoBattleResolver skips encounters without returning early`() {
        val encounters = listOf(
            Encounter(index = 0, motifId = 1, atOffsetMin = 20),
            Encounter(index = 1, motifId = 3, atOffsetMin = 40),
        )

        val outcome = NoBattleResolver.resolve(
            encounters = encounters,
            party = noModifierParty(3),
            snapshot = noModifierSnapshot,
            rng = Rng(10),
        )

        assertEquals(encounters, outcome.encounters.map { it.encounter })
        assertTrue(outcome.encounters.all { it.success == null })
        assertEquals(0, outcome.q)
        assertFalse(outcome.earlyReturn)
    }

    @Test fun `MinimalBattleResolver reports a success or failure for each encounter`() {
        val encounters = listOf(
            Encounter(index = 0, motifId = 1, atOffsetMin = 20),
            Encounter(index = 1, motifId = 3, atOffsetMin = 40),
        )

        val outcome = resolver.resolve(
            encounters = encounters,
            party = noModifierParty(3),
            snapshot = noModifierSnapshot,
            rng = Rng(subSeed(10, "quell")),
        )

        assertEquals(encounters, outcome.encounters.map { it.encounter })
        assertTrue(outcome.encounters.all { it.success != null })
        assertEquals(outcome.encounters.count { it.success == true }, outcome.q)
        assertFalse(outcome.earlyReturn)
    }
}
