package dev.seirei.core.roll

import dev.seirei.core.model.Party
import dev.seirei.core.model.PartyMember
import dev.seirei.core.model.Personality
import org.junit.Assert.assertEquals
import org.junit.Test

class ManaTest {
    @Test fun `hoarder adds ten percent to mana`() {
        val party = partyWith(Personality.HOARDER)
        val result = RollEngine.resolve(
            seed = 1L,
            snapshot = noModifierSnapshot,
            party = party,
            constants = RollConstants.POC.copy(encounterRatePerHour = 0.0),
        )
        assertEquals(22, result.manaGained)
    }

    @Test fun `greedy adds thirty percent to mana`() {
        val party = partyWith(Personality.GREEDY)
        val result = RollEngine.resolve(
            seed = 1L,
            snapshot = noModifierSnapshot,
            party = party,
            constants = RollConstants.POC.copy(encounterRatePerHour = 0.0),
        )
        assertEquals(26, result.manaGained)
    }

    private fun partyWith(personality: Personality) = Party(
        listOf(PartyMember(motifId = 1, star = 3, personalityId = personality.id)),
    )
}
