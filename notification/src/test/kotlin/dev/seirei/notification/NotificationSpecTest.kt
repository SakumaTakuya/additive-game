package dev.seirei.notification

import dev.seirei.core.model.ColorTier
import dev.seirei.core.testing.aResident
import dev.seirei.core.ui.NotifUiState
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NotificationSpec の組み立て（設計書04 §4.1・§4.4、設計書01 §6.3）。
 * NotifUiState から純データだけを作る。Android への写像は SeireiNotifications が担う。
 */
class NotificationSpecTest {
    private val startedAt: Instant = Instant.parse("2026-10-25T00:00:00Z")
    private val endAt: Instant = startedAt.plusSeconds(7_200)

    @Test
    fun `探索中はProgressStyleの各要素とカウントダウンが入る`() {
        val leader = aResident(id = 1L, motifId = 4, colorTier = ColorTier.RARE)
        val spec = notificationSpec(
            NotifUiState.Exploring(startedAt, endAt, progress = 0.25, leader = leader),
        )

        assertEquals(NotificationChannels.EXPLORATION_ID, spec?.channelId)
        assertEquals("探索中", spec?.title)
        assertEquals(NotificationIcon.LANTERN_SILHOUETTE, spec?.smallIcon)
        assertEquals(NotificationIcon.GATE_START, spec?.startIcon)
        assertEquals(NotificationIcon.HOME_END, spec?.endIcon)
        assertEquals(TrackerSpec.ResidentSprite(motifId = 4, colorTier = ColorTier.RARE), spec?.tracker)
        assertEquals(0.25, spec?.progress ?: -1.0, 1e-9)
        assertEquals(endAt.toEpochMilli(), spec?.whenMillis)
        assertTrue(spec?.ongoing == true)
        assertTrue(spec?.requestPromoted == true)
        assertEquals("app://home", spec?.deepLink)
    }

    @Test
    fun `編成が空ならトラッカーはランタンになる`() {
        val spec = notificationSpec(
            NotifUiState.Exploring(startedAt, endAt, progress = 0.0, leader = null),
        )

        assertEquals(TrackerSpec.Lantern, spec?.tracker)
    }

    @Test
    fun `帰還時は帰ってきた通知になり、タップで回収へ進む`() {
        val spec = notificationSpec(NotifUiState.Returned)

        assertEquals(NotificationChannels.RETURN_ID, spec?.channelId)
        assertEquals("精霊が帰ってきた", spec?.title)
        assertEquals(NotificationIcon.LANTERN_SILHOUETTE, spec?.smallIcon)
        assertEquals(1.0, spec?.progress ?: -1.0, 1e-9)
        assertFalse(spec?.ongoing == true)
        assertEquals("app://collect", spec?.deepLink)
        assertNull(spec?.whenMillis)
    }

    @Test
    fun `IDLEは通知なし`() {
        assertNull(notificationSpec(null))
    }
}
