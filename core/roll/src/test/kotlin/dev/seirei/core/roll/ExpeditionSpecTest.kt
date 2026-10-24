package dev.seirei.core.roll

import dev.seirei.core.model.Material
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 探索1回の流れ（設計書02 §4）。:core:roll を読むときの入口。
 *
 * `RollEngine.resolve(seed, snapshot, party, constants)` は
 * ① 遭遇生成 → ② 鎮め → ③ 覚醒数 → ④ 個体ロール → ⑤ ログ・魔力集計 を固定順で行い、
 * 「ログ」「覚醒した住人」「持ち帰る魔力」を返す。
 *
 * 各段の詳細：① EncounterSpecTest ／ ② QuellSpecTest・ArrowsSpecTest ／ ③④ AwakeningSpecTest ／ ⑤ ManaSpecTest。
 * 決定論・統計・凍結値などの保証は guarantee/ にある。
 */
class ExpeditionSpecTest {
    private val constants = RollConstants.POC

    // ---- 代表例 ----

    @Test fun `探索1回で、遭遇ごとの鎮めの成否と覚醒した住人と持ち帰る魔力が決まる`() {
        val result = RollEngine.resolve(seed = 42, snapshot = neutralSnapshot, party = neutralParty(3), constants = constants)

        val encountered = result.log.filterIsInstance<LogEvent.Encountered>()
        val quelled = result.log.filterIsInstance<LogEvent.Quelled>()
        val failed = result.log.filterIsInstance<LogEvent.Failed>()
        // ① 遭遇はシードから決まる
        assertEquals(generateEncounters(42, constants).size, encountered.size)
        // ② 遭遇ごとに「鎮めた」か「失敗」のどちらか
        assertEquals(encountered.size, quelled.size + failed.size)
        // ③④ 少なくとも1体は覚醒し、ログの最後に覚醒が並ぶ
        assertTrue(result.awakened.isNotEmpty())
        assertEquals(result.awakened.indices.map { LogEvent.Awakened(it) }, result.log.takeLast(result.awakened.size))
        // ⑤ 相性なしの編成なので、魔力は 基本量20 ＋ 鎮め1回につき10
        assertEquals(20 + 10 * quelled.size, result.manaGained)
        // PoC では早期帰還は起きない（DEC-04）
        assertFalse(result.earlyReturn)
    }

    // ---- バリエーション ----

    @Test fun `空編成では鎮めが一度も成功せず、覚醒は1体、魔力は基本量の20だけ`() {
        val result = RollEngine.resolve(seed = 42, snapshot = neutralSnapshot, party = emptyParty, constants = constants)

        assertTrue(result.log.none { it is LogEvent.Quelled })
        assertEquals(1, result.awakened.size)
        assertEquals(20, result.manaGained)
    }

    // ---- ログの形（⑤） ----

    @Test fun `鎮めた遭遇は「遭遇→鎮めた→見つけた」、失敗した遭遇は「遭遇→失敗」とログに残る`() {
        val quelled = EncounterOutcome(Encounter(index = 0, motifId = motifId(Material.WATER), atOffsetMin = 12), success = true)
        val failed = EncounterOutcome(Encounter(index = 1, motifId = motifId(Material.BONE), atOffsetMin = 20), success = false)

        assertEquals(
            listOf(
                LogEvent.Encountered(index = 0, motifId = motifId(Material.WATER), atOffsetMin = 12),
                LogEvent.Quelled(index = 0),
                LogEvent.Found(index = 0, mana = 10),
                LogEvent.Encountered(index = 1, motifId = motifId(Material.BONE), atOffsetMin = 20),
                LogEvent.Failed(index = 1),
            ),
            battleLogEvents(listOf(quelled, failed), manaPerQuell = 10),
        )
    }

    @Test fun `スキップした遭遇は「遭遇」だけが残り、失敗と区別できる（イベント型は増やさない）`() {
        val skipped = EncounterOutcome(Encounter(index = 0, motifId = motifId(Material.WATER), atOffsetMin = 12), success = null)

        assertEquals(
            listOf(LogEvent.Encountered(index = 0, motifId = motifId(Material.WATER), atOffsetMin = 12)),
            battleLogEvents(listOf(skipped), manaPerQuell = 10),
        )
    }
}
