package dev.seirei.core.roll

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogShapeTest {
    @Test fun `ten thousand seeds follow encounter and awakening event grammar`() {
        repeat(10_000) { seed ->
            val result = RollEngine.resolve(seed.toLong(), noModifierSnapshot, noModifierParty(3), RollConstants.POC)
            var cursor = 0
            var encounterIndex = 0
            while (cursor < result.log.size && result.log[cursor] !is LogEvent.Awakened) {
                val encountered = result.log[cursor++] as LogEvent.Encountered
                assertEquals(encounterIndex, encountered.index)
                val outcome = result.log[cursor++]
                when (outcome) {
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
