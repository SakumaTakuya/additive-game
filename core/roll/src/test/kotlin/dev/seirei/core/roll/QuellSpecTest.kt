package dev.seirei.core.roll

import dev.seirei.core.model.Material
import dev.seirei.core.model.Personality
import dev.seirei.core.model.TimeBand
import dev.seirei.core.model.WeatherKind
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ② 鎮め解決（設計書02 §5.1・§5.2）。
 *
 * 勝率は編成全体で1つ：
 * `p = min(0.90, max(0, Σ(0.12 + 0.02×(★−1) + 0.05×鎮めの矢印)))`
 * 各遭遇は `quellRng.nextDouble() < p` で鎮め成功。矢印の本数は ArrowsSpecTest。
 */
class QuellSpecTest {
    private val resolver = MinimalBattleResolver(RollConstants.POC)

    // ---- 代表例 ----

    @Test fun `1体・★3・相性なしの勝率は16%`() {
        // 0.12 + 0.02×(3−1) + 0.05×0
        assertEquals(0.16, resolver.winRate(neutralParty(3), neutralSnapshot), 1e-12)
    }

    // ---- バリエーション ----

    @Test fun `人数が増えるほど勝率は上がる：3体・★2,3,3で46%`() {
        // (0.12+0.02) + (0.12+0.04) + (0.12+0.04)
        assertEquals(0.46, resolver.winRate(neutralParty(2, 3, 3), neutralSnapshot), 1e-12)
    }

    @Test fun `鎮めの矢印1本につき勝率が5ポイント上がる`() {
        val calm = partyOf(member(Material.WATER, 3, Personality.CALM)) // 穏やか：いつでも矢印+1

        // 0.16 + 0.05×1
        assertEquals(0.21, resolver.winRate(calm, neutralSnapshot), 1e-12)
    }

    @Test fun `遭遇ごとに成否が決まり、成功した数がqになる`() {
        val encounters = listOf(
            Encounter(index = 0, motifId = motifId(Material.WATER), atOffsetMin = 20),
            Encounter(index = 1, motifId = motifId(Material.EARTHENWARE), atOffsetMin = 40),
        )

        val outcome = resolver.resolve(encounters, neutralParty(3), neutralSnapshot, Rng(subSeed(10, "quell")))

        assertEquals(encounters, outcome.encounters.map { it.encounter })
        assertTrue(outcome.encounters.all { it.success != null })
        assertEquals(outcome.encounters.count { it.success == true }, outcome.q)
        assertFalse(outcome.earlyReturn) // PoC では早期帰還を起こさない（DEC-04）
    }

    @Test fun `NoBattleResolverは全遭遇をスキップし、qは0`() {
        val encounters = listOf(
            Encounter(index = 0, motifId = motifId(Material.WATER), atOffsetMin = 20),
            Encounter(index = 1, motifId = motifId(Material.EARTHENWARE), atOffsetMin = 40),
        )

        val outcome = NoBattleResolver.resolve(encounters, neutralParty(3), neutralSnapshot, Rng(10))

        assertEquals(encounters, outcome.encounters.map { it.encounter })
        assertTrue(outcome.encounters.all { it.success == null })
        assertEquals(0, outcome.q)
        assertFalse(outcome.earlyReturn)
    }

    // ---- 境界 ----

    @Test fun `空編成の勝率は0（鎮める者がいない）`() {
        assertEquals(0.0, resolver.winRate(emptyParty, neutralSnapshot), 1e-12)
    }

    @Test fun `1体いれば勝率は正：最悪の★1・欲張り・雨の日の火でも2%`() {
        val worst = partyOf(member(Material.FIRE, 1, Personality.GREEDY))
        val rainyThursday = snapshot(WeatherKind.RAIN, TimeBand.EVENING, DayOfWeek.THURSDAY)

        // 0.12 + 0.02×0 + 0.05×(−2)
        assertEquals(0.02, resolver.winRate(worst, rainyThursday), 1e-12)
    }

    @Test fun `勝率の上限は90%：素の合計が105%でも90%に抑える`() {
        // ★5×3 で 0.20×3 = 0.60、矢印は 隊長1本＋のんびり屋4本×2 = 9本で +0.45
        assertEquals(0.90, resolver.winRate(strongestParty, strongWeekendSnapshot), 1e-12)
    }
}
