package dev.seirei.core.roll.guarantee

import dev.seirei.core.model.ColorTier
import dev.seirei.core.model.ContextSnapshot
import dev.seirei.core.model.Party
import dev.seirei.core.roll.MinimalBattleResolver
import dev.seirei.core.roll.RollConstants
import dev.seirei.core.roll.Rng
import dev.seirei.core.roll.emptyParty
import dev.seirei.core.roll.generateEncounters
import dev.seirei.core.roll.neutralParty
import dev.seirei.core.roll.neutralSnapshot
import dev.seirei.core.roll.rollAwakened
import dev.seirei.core.roll.strongWeekendSnapshot
import dev.seirei.core.roll.strongestParty
import dev.seirei.core.roll.subSeed
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 統計（設計書02 §5.2・§5.4・§7・§8）。多数のシードで実測した分布が設計書の表に収まること。
 * 期待値は設計書の表の値をそのまま写している。ここが落ちたら、定数か実装のどちらかが表からずれている。
 */
class StatisticsTest {
    private val constants = RollConstants.POC

    @Test fun `1探索あたりの遭遇は平均6回（POC・DEMOとも）`() {
        listOf(RollConstants.POC, RollConstants.DEMO).forEach { constants ->
            val mean = (0 until 10_000)
                .sumOf { seed -> generateEncounters(seed.toLong(), constants).size }
                .toDouble() / 10_000

            assertEquals(6.0, mean, 0.1)
        }
    }

    /** 設計書02 §5.2 の表の「鎮め数 E[q]」列。 */
    @Test fun `編成ごとの平均鎮め数は設計書の表どおり`() {
        val cases = listOf(
            Triple(emptyParty, neutralSnapshot, 0.00),
            Triple(neutralParty(3), neutralSnapshot, 0.96),
            Triple(neutralParty(2, 3, 3), neutralSnapshot, 2.76),
            Triple(strongestParty, strongWeekendSnapshot, 5.39),
        )

        cases.forEach { (party, snapshot, expectedMean) ->
            assertEquals(expectedMean, meanQuellCount(party, snapshot), 0.05)
        }
    }

    /** 設計書02 §5.4：★4+ 15%・★5 3% から、鎮め5回以上で ★5 10% へ。 */
    @Test fun `★の分布は鎮めがなければ素の分布、5回で最大まで上振れる`() {
        val base = stars(quellCount = 0)
        assertEquals(15.0, base.count { it >= 4 } * 100.0 / base.size, 0.5)
        assertEquals(3.0, base.count { it == 5 } * 100.0 / base.size, 0.3)

        val boosted = stars(quellCount = 5)
        assertEquals(10.0, boosted.count { it == 5 } * 100.0 / boosted.size, 0.5)
    }

    /** 設計書02 §7：通常91%・レア8%・発光1%（DEMO は 75/15/10）。 */
    @Test fun `色の分布は通常・レア・発光の目標比率どおり`() {
        assertColorRates(RollConstants.POC, normal = 91.0, rare = 8.0, glow = 1.0)
        assertColorRates(RollConstants.DEMO, normal = 75.0, rare = 15.0, glow = 10.0)
    }

    private fun meanQuellCount(party: Party, snapshot: ContextSnapshot): Double {
        val resolver = MinimalBattleResolver(constants)
        val total = (0 until SAMPLE_COUNT).sumOf { seed ->
            resolver.resolve(
                encounters = generateEncounters(seed.toLong(), constants),
                party = party,
                snapshot = snapshot,
                rng = Rng(subSeed(seed.toLong(), "quell")),
            ).q
        }
        return total.toDouble() / SAMPLE_COUNT
    }

    private fun stars(quellCount: Int): List<Int> = (0 until SAMPLE_COUNT).flatMap { seed ->
        rollAwakened(seed.toLong(), quellCount, false, constants).map { it.star }
    }

    private fun assertColorRates(constants: RollConstants, normal: Double, rare: Double, glow: Double) {
        val colors = (0 until SAMPLE_COUNT).flatMap { seed ->
            rollAwakened(seed.toLong(), 0, false, constants).map { it.color }
        }
        assertEquals(normal, colors.count { it == ColorTier.NORMAL } * 100.0 / colors.size, 0.5)
        assertEquals(rare, colors.count { it == ColorTier.RARE } * 100.0 / colors.size, 0.5)
        assertEquals(glow, colors.count { it == ColorTier.GLOW } * 100.0 / colors.size, 0.5)
    }

    private companion object {
        const val SAMPLE_COUNT = 100_000
    }
}
