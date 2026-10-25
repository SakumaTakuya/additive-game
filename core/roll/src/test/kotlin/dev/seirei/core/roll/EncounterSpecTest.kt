package dev.seirei.core.roll

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ① 遭遇生成（設計書02 §4）。
 * 遭遇数はポアソン分布（平均＝遭遇率×探索時間。PoC は 3回/時×2時間＝6回）、相手は10種のモチーフから等確率（DEC-06）。
 * 平均が6回になることは guarantee/StatisticsTest で確かめる。
 */
class EncounterSpecTest {

    // ---- 代表例 ----

    @Test fun `遭遇は探索時間内に時刻順で並び、0から連番が振られる`() {
        val constants = RollConstants.POC
        val encounters = generateEncounters(seed = 0x5EED, constants = constants)

        assertEquals(encounters.indices.toList(), encounters.map { it.index })
        assertEquals(encounters.sortedBy { it.atOffsetMin }, encounters)
        assertTrue(encounters.all { it.atOffsetMin in 0 until 120 })
    }

    // ---- バリエーション ----

    @Test fun `遭遇相手は10種のモチーフのどれか`() {
        val encounters = (0L until 100L).flatMap { generateEncounters(it, RollConstants.POC) }

        assertTrue(encounters.all { it.motifId in 1..10 })
    }

    @Test fun `デモ定数では探索3分の中に収まる`() {
        val encounters = generateEncounters(seed = 0x5EED, constants = RollConstants.DEMO)

        assertTrue(encounters.isNotEmpty())
        assertTrue(encounters.all { it.atOffsetMin in 0 until 3 })
    }

    // ---- 境界 ----

    @Test fun `遭遇率が0なら遭遇は起きない`() {
        val constants = RollConstants.POC.copy(encounterRatePerHour = 0.0)

        assertEquals(emptyList<Encounter>(), generateEncounters(seed = 42, constants = constants))
    }

    // ---- 異常系 ----

    @Test fun `探索時間が0分なら例外`() {
        val constants = RollConstants.POC.copy(expeditionMinutes = 0)

        assertThrows(IllegalArgumentException::class.java) { generateEncounters(seed = 42, constants = constants) }
    }
}
