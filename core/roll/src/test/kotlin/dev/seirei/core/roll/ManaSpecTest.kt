package dev.seirei.core.roll

import dev.seirei.core.model.Material
import dev.seirei.core.model.Personality
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ⑤ 持ち帰る魔力（設計書02 §5.3）。
 * `manaGained = floor((基本量20 + 10×鎮め数) × (1 + 0.10×編成全体の魔力の矢印))`
 * 基本量・鎮め1回あたりの量は DEC-03 の暫定値。
 */
class ManaSpecTest {
    /** 遭遇が起きないようにして、鎮め数を0に固定する。 */
    private val noEncounter = RollConstants.POC.copy(encounterRatePerHour = 0.0)

    private fun manaFor(vararg personalities: Personality): Int {
        val party = partyOf(*personalities.map { member(Material.WATER, 3, it) }.toTypedArray())
        return RollEngine.resolve(seed = 1, snapshot = neutralSnapshot, party = party, constants = noEncounter).manaGained
    }

    // ---- 代表例 ----

    @Test fun `相性なしなら、魔力は基本量20に鎮め1回ごと10を足した量`() {
        val result = RollEngine.resolve(seed = 42, snapshot = neutralSnapshot, party = neutralParty(3), constants = RollConstants.POC)
        val quelled = result.log.count { it is LogEvent.Quelled }

        assertEquals(20 + 10 * quelled, result.manaGained)
    }

    // ---- 魔力の矢印 ----

    @Test fun `ためこみ屋1体で1割増し：20が22になる`() {
        assertEquals(22, manaFor(Personality.HOARDER))
    }

    @Test fun `欲張り1体で3割増し：20が26になる`() {
        assertEquals(26, manaFor(Personality.GREEDY))
    }

    @Test fun `魔力の矢印は編成全体で合算する：ためこみ屋2体で2割増し`() {
        assertEquals(24, manaFor(Personality.HOARDER, Personality.HOARDER))
    }

    // ---- 境界 ----

    @Test fun `遭遇がなく相性もなければ基本量の20だけ`() {
        assertEquals(20, manaFor(Personality.NOCTURNAL))
    }
}
