package dev.seirei.ruins

import org.junit.Assert.assertEquals
import org.junit.Test

class FlavorConfigTest {
    @Test
    fun usesPocExpeditionDuration() {
        assertEquals(120, FlavorConfig.rollConstants.expeditionMinutes)
    }
}
