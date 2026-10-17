package dev.seirei.core.roll

import dev.seirei.core.model.Party
import org.junit.Assert.assertEquals
import org.junit.Test

class QuellStatsTest {
    private val constants = RollConstants.POC
    private val resolver = MinimalBattleResolver(constants)

    @Test fun `mean quelled encounters over one hundred thousand expeditions matches design`() {
        val cases = listOf(
            Case(Party(emptyList()), noModifierSnapshot, expectedMean = 0.00),
            Case(noModifierParty(3), noModifierSnapshot, expectedMean = 0.96),
            Case(noModifierParty(2, 3, 3), noModifierSnapshot, expectedMean = 2.76),
            Case(cappedParty, weekendSnapshot, expectedMean = 5.39),
        )

        cases.forEach { case ->
            val total = (0 until SAMPLE_COUNT).sumOf { seed ->
                val encounters = generateEncounters(seed.toLong(), constants)
                resolver.resolve(
                    encounters = encounters,
                    party = case.party,
                    snapshot = case.snapshot,
                    rng = Rng(subSeed(seed.toLong(), "quell")),
                ).q
            }

            assertEquals(case.expectedMean, total.toDouble() / SAMPLE_COUNT, 0.05)
        }
    }

    private data class Case(
        val party: Party,
        val snapshot: dev.seirei.core.model.ContextSnapshot,
        val expectedMean: Double,
    )

    private companion object {
        const val SAMPLE_COUNT = 100_000
    }
}
