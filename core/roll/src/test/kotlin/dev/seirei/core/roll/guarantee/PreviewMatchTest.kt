package dev.seirei.core.roll.guarantee

import dev.seirei.core.roll.MinimalBattleResolver
import dev.seirei.core.roll.RollConstants
import dev.seirei.core.roll.arrowsFor
import dev.seirei.core.roll.strongWeekendSnapshot
import dev.seirei.core.roll.strongestParty
import org.junit.Assert.assertEquals
import org.junit.Test

/** プレビュー一致（設計書02 §5.3 原則2・§8）。編成画面に出す矢印と、鎮め解決が内部で使う矢印が同じであること。 */
class PreviewMatchTest {
    @Test fun `鎮め解決はプレビューと同じarrowsForの値で勝率を計算する`() {
        val constants = RollConstants.POC
        val party = strongestParty
        val snapshot = strongWeekendSnapshot
        val previewQuellArrows = party.members.indices.map { slot -> arrowsFor(slot, party, snapshot).quell }
        val resolver = MinimalBattleResolver(constants)

        assertEquals(previewQuellArrows, resolver.quellArrowsFor(party, snapshot))
        assertEquals(
            party.members.indices.sumOf {
                constants.memberBase +
                    constants.starPt * (party.members[it].star - 1) +
                    constants.arrowPt * previewQuellArrows[it]
            }.coerceAtMost(constants.pMax),
            resolver.winRate(party, snapshot),
            1e-12,
        )
    }
}
