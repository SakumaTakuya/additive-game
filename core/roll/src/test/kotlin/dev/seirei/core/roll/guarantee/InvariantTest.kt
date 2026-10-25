package dev.seirei.core.roll.guarantee

import dev.seirei.core.roll.LogEvent
import dev.seirei.core.roll.RollConstants
import dev.seirei.core.roll.RollEngine
import dev.seirei.core.roll.neutralParty
import dev.seirei.core.roll.neutralSnapshot
import dev.seirei.core.roll.rollAwakened
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 不変条件（設計書02 §4・§8）。任意のシード1万件で、結果の値域とログの文法が崩れないこと。 */
class InvariantTest {
    @Test fun `覚醒は必ず1体以上で、★は1〜5・モチーフは1〜10・性格は12種のどれか`() {
        repeat(10_000) { seed ->
            val residents = rollAwakened(seed.toLong(), seed % 6, false, RollConstants.POC)
            assertTrue("seed=$seed must awaken at least one", residents.isNotEmpty())
            residents.forEach { resident ->
                assertTrue("star=${resident.star}", resident.star in 1..5)
                assertTrue("motifId=${resident.motifId}", resident.motifId in 1..10)
                assertTrue("personalityId=${resident.personalityId}", resident.personalityId in 0..11)
            }
        }
    }

    /** 1遭遇 = Encountered → (Quelled → Found | Failed)。全遭遇の後に Awakened が覚醒数だけ並ぶ（02 §4 出力型）。 */
    @Test fun `ログは遭遇ごとの文法に従い、最後に覚醒が並ぶ`() {
        repeat(10_000) { seed ->
            val result = RollEngine.resolve(seed.toLong(), neutralSnapshot, neutralParty(3), RollConstants.POC)
            var cursor = 0
            var encounterIndex = 0
            while (cursor < result.log.size && result.log[cursor] !is LogEvent.Awakened) {
                val encountered = result.log[cursor++] as LogEvent.Encountered
                assertEquals(encounterIndex, encountered.index)
                when (val outcome = result.log[cursor++]) {
                    is LogEvent.Quelled -> {
                        assertEquals(encounterIndex, outcome.index)
                        val found = result.log[cursor++] as LogEvent.Found
                        assertEquals(encounterIndex, found.index)
                        assertEquals(RollConstants.POC.manaPerQuell, found.mana)
                    }
                    is LogEvent.Failed -> assertEquals(encounterIndex, outcome.index)
                    else -> throw AssertionError("Expected Quelled or Failed, got $outcome")
                }
                encounterIndex++
            }

            val awakenings = result.log.drop(cursor).map { it as LogEvent.Awakened }
            assertEquals(result.awakened.indices.map { LogEvent.Awakened(it) }, awakenings)
            assertTrue(result.awakened.isNotEmpty())
        }
    }
}
