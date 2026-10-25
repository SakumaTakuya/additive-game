package dev.seirei.core.roll.guarantee

import dev.seirei.core.roll.RollConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 定数テーブルの健全性とピン留め（設計書02 §6・§8 不変条件）。
 * 定数を変えるときは AGENTS.md §8 の手順（tools/roll_sim.py で確認 → RollConstants → このテスト）に従う。
 */
class RollConstantsTest {
    private val flavors = listOf(RollConstants.POC, RollConstants.DEMO)

    @Test fun `★の重みはPOC・DEMOとも合計1`() {
        flavors.forEach { constants ->
            assertEquals(1.0, constants.starBase.sum(), 1e-12)
            assertEquals(1.0, constants.starBoosted.sum(), 1e-12)
            assertEquals(1.0, constants.star1To3.sum(), 1e-12)
        }
    }

    @Test fun `確率の定数はすべて0以上1以下`() {
        flavors.forEach { constants ->
            val probabilities = listOf(
                constants.starBase.first, constants.starBase.second, constants.starBase.third,
                constants.starBoosted.first, constants.starBoosted.second, constants.starBoosted.third,
                constants.star1To3.first, constants.star1To3.second, constants.star1To3.third,
                constants.rareColorRate,
                constants.glowColorRate,
                constants.manaArrowRate,
                constants.memberBase,
                constants.starPt,
                constants.arrowPt,
                constants.pMax,
            ) + constants.awakenUpsideTable

            probabilities.forEach { probability ->
                assertTrue("$probability should be in 0..1", probability in 0.0..1.0)
            }
        }
    }

    /** tools/roll_sim.py の入力と同じ値であることを固定する。意図した変更のときだけ書き換える。 */
    @Test fun `POCの値はロールシミュレーションの入力と一致する`() {
        val poc = RollConstants.POC

        assertEquals(120, poc.expeditionMinutes)
        assertEquals(3.0, poc.encounterRatePerHour, 0.0)
        assertEquals(0.85, poc.starBase.first, 0.0)
        assertEquals(0.12, poc.starBase.second, 0.0)
        assertEquals(0.03, poc.starBase.third, 0.0)
        assertEquals(0.70, poc.starBoosted.first, 0.0)
        assertEquals(0.20, poc.starBoosted.second, 0.0)
        assertEquals(0.10, poc.starBoosted.third, 0.0)
        assertEquals(5, poc.quellNorm)
        assertEquals(0.08, poc.rareColorRate, 0.0)
        assertEquals(0.01, poc.glowColorRate, 0.0)
        assertEquals(0.12, poc.memberBase, 0.0)
        assertEquals(0.02, poc.starPt, 0.0)
        assertEquals(0.05, poc.arrowPt, 0.0)
        assertEquals(0.90, poc.pMax, 0.0)
        assertEquals(0.10, poc.manaArrowRate, 0.0)
        assertEquals(12, poc.personalityCount)
    }
}
