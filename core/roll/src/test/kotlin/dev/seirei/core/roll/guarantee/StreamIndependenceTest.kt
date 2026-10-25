package dev.seirei.core.roll.guarantee

import dev.seirei.core.roll.MinimalBattleResolver
import dev.seirei.core.roll.NoBattleResolver
import dev.seirei.core.roll.ResidentRoll
import dev.seirei.core.roll.RollConstants
import dev.seirei.core.roll.Rng
import dev.seirei.core.model.Material
import dev.seirei.core.roll.generateEncounters
import dev.seirei.core.roll.motifId
import dev.seirei.core.roll.neutralParty
import dev.seirei.core.roll.neutralSnapshot
import dev.seirei.core.roll.resolveNamed
import dev.seirei.core.roll.rollAwakened
import dev.seirei.core.roll.subSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ストリーム独立（設計書02 §3・§8）。乱数はサブシステムごとに別ストリームなので、
 * 遭遇の定数だけを変えても覚醒する個体の属性は変わらないこと。
 */
class StreamIndependenceTest {
    private val fewEncounters = RollConstants.POC.copy(encounterRatePerHour = 1.0)
    private val manyEncounters = RollConstants.POC.copy(encounterRatePerHour = 300.0)

    @Test fun `鎮めがなければ、遭遇率を変えても覚醒結果はまったく同じ`() {
        repeat(1_000) { seed ->
            assertEquals(rollWithoutBattle(seed.toLong(), fewEncounters), rollWithoutBattle(seed.toLong(), manyEncounters))
        }
    }

    @Test fun `鎮めがあれば★だけが変わり、モチーフ・性格・色は変わらない`() {
        repeat(1_000) { seed ->
            val few = rollWithMinimalBattle(seed.toLong(), fewEncounters)
            val many = rollWithMinimalBattle(seed.toLong(), manyEncounters)
            few.zip(many).forEach { (left, right) ->
                assertEquals(left.motifId, right.motifId)
                assertEquals(left.personalityId, right.personalityId)
                assertEquals(left.color, right.color)
            }
        }
    }

    /** 設計書02 §4：指名覚醒は named.* ストリームを使うので、同じシードの通常覚醒とは独立に抽選される。 */
    @Test fun `指名覚醒は同じシードの通常覚醒の1体目と星・性格・色が変わりうる`() {
        val named = (0L until 100L).map { seed ->
            resolveNamed(seed, motifId(Material.FIRE), RollConstants.POC)
        }
        val firstAwakened = (0L until 100L).map { seed ->
            rollAwakened(seed, quellCount = 0, earlyReturn = false, constants = RollConstants.POC).first()
        }

        assertTrue(named.zip(firstAwakened).any { (left, right) -> left.star != right.star })
        assertTrue(named.zip(firstAwakened).any { (left, right) -> left.personalityId != right.personalityId })
        assertTrue(named.zip(firstAwakened).any { (left, right) -> left.color != right.color })
    }

    private fun rollWithoutBattle(seed: Long, constants: RollConstants): List<ResidentRoll> {
        val outcome = NoBattleResolver.resolve(
            encounters = generateEncounters(seed, constants),
            party = neutralParty(),
            snapshot = neutralSnapshot,
            rng = Rng(subSeed(seed, "quell")),
        )
        return rollAwakened(seed, outcome.q, false, constants)
    }

    private fun rollWithMinimalBattle(seed: Long, constants: RollConstants): List<ResidentRoll> {
        val outcome = MinimalBattleResolver(constants).resolve(
            encounters = generateEncounters(seed, constants),
            party = neutralParty(3, 3, 3),
            snapshot = neutralSnapshot,
            rng = Rng(subSeed(seed, "quell")),
        )
        return rollAwakened(seed, outcome.q, outcome.earlyReturn, constants)
    }
}
