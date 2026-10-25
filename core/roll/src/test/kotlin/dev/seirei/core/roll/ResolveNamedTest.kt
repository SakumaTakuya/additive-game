package dev.seirei.core.roll

import dev.seirei.core.model.Material
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 指名覚醒API（設計書02 §4「指名覚醒API」）。
 *
 * モチーフは引数で確定し、★・性格・色は通常ロール（★は鎮め補正なしの `starBase` 分布）。
 * ★5率の実測は guarantee/StatisticsTest、通常覚醒とのストリーム分離は guarantee/StreamIndependenceTest。
 */
class ResolveNamedTest {
    private val constants = RollConstants.POC

    // ---- 代表例 ----

    @Test fun `モチーフは引数で指定したとおりになる`() {
        val motifs = listOf(motifId(Material.FIRE), motifId(Material.WATER), motifId(Material.PLANT))

        motifs.forEach { motifId ->
            (0L until 100L).forEach { seed ->
                assertEquals("seed=$seed", motifId, resolveNamed(seed, motifId, constants).motifId)
            }
        }
    }

    @Test fun `同じシードとモチーフなら同じ個体になる`() {
        (0L until 100L).forEach { seed ->
            val first = resolveNamed(seed, motifId(Material.FIRE), constants)
            val second = resolveNamed(seed, motifId(Material.FIRE), constants)

            assertEquals("seed=$seed", first, second)
        }
    }

    // ---- バリエーション ----

    @Test fun `シードが違えば星・性格・色が変わりうる`() {
        val rolls = (0L until 200L).map { seed -> resolveNamed(seed, motifId(Material.FIRE), constants) }

        assertTrue(rolls.map { it.star }.distinct().size > 1)
        assertTrue(rolls.map { it.personalityId }.distinct().size > 1)
        assertTrue(rolls.map { it.color }.distinct().size > 1)
    }

    @Test fun `星は1から5、性格は定数の範囲内になる`() {
        (0L until 200L).forEach { seed ->
            val roll = resolveNamed(seed, motifId(Material.FIRE), constants)

            assertTrue("seed=$seed", roll.star in 1..5)
            assertTrue("seed=$seed", roll.personalityId in 0 until constants.personalityCount)
        }
    }
}
