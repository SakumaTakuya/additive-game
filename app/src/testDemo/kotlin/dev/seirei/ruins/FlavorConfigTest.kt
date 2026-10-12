package dev.seirei.ruins

import org.junit.Assert.assertEquals
import org.junit.Test

class FlavorConfigTest {
    @Test
    fun usesDemoExpeditionDuration() {
        assertEquals(3, FlavorConfig.rollConstants.expeditionMinutes)
    }
}
