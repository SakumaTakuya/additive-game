package dev.seirei.core.roll

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamIndependenceTest {
    @Test fun `encounter rate does not affect no battle awakenings`() {
        val lowEncounter = RollConstants.POC.copy(encounterRatePerHour = 1.0)
        val highEncounter = RollConstants.POC.copy(encounterRatePerHour = 300.0)
        repeat(1_000) { seed ->
            val first = rollAwakened(seed.toLong(), NoBattleResolver.resolve(
                encounters = generateEncounters(seed.toLong(), lowEncounter),
                party = noModifierParty(), snapshot = noModifierSnapshot,
                rng = Rng(subSeed(seed.toLong(), "quell")),
            ).q, false, lowEncounter)
            val second = rollAwakened(seed.toLong(), NoBattleResolver.resolve(
                encounters = generateEncounters(seed.toLong(), highEncounter),
                party = noModifierParty(), snapshot = noModifierSnapshot,
                rng = Rng(subSeed(seed.toLong(), "quell")),
            ).q, false, highEncounter)
            assertEquals(first, second)
        }
    }

    @Test fun `minimal battle changes star only for residents present in both results`() {
        val lowEncounter = RollConstants.POC.copy(encounterRatePerHour = 1.0)
        val highEncounter = RollConstants.POC.copy(encounterRatePerHour = 300.0)
        repeat(1_000) { seed ->
            val first = rollForMinimalBattle(seed.toLong(), lowEncounter)
            val second = rollForMinimalBattle(seed.toLong(), highEncounter)
            first.zip(second).forEach { (left, right) ->
                assertEquals(left.motifId, right.motifId)
                assertEquals(left.personalityId, right.personalityId)
                assertEquals(left.color, right.color)
            }
        }
    }

    private fun rollForMinimalBattle(seed: Long, constants: RollConstants): List<ResidentRoll> {
        val encounters = generateEncounters(seed, constants)
        val outcome = MinimalBattleResolver(constants).resolve(
            encounters = encounters,
            party = noModifierParty(3, 3, 3),
            snapshot = noModifierSnapshot,
            rng = Rng(subSeed(seed, "quell")),
        )
        return rollAwakened(seed, outcome.q, outcome.earlyReturn, constants)
    }
}
