package dev.seirei.core.ui

import dev.seirei.core.model.ColorTier
import dev.seirei.core.model.ContextSnapshot
import dev.seirei.core.model.ExplorationSession
import dev.seirei.core.model.Resident
import dev.seirei.core.model.TimeBand
import dev.seirei.core.model.WeatherKind
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 通知の UiState マッパー（設計書01 §6.1、設計書04 §2・§4.1）。
 *
 * IDLE は通知なし（null）。EXPLORING は進捗（0..1 にクランプ）・`endAt`・隊長を持ち、
 * 隊長がいなければ null のまま渡してランタンへのフォールバックを表す。RETURNED は帰還通知。
 */
class NotificationMapperTest {
    private val t0: Instant = Instant.parse("2026-10-25T00:00:00Z")
    private val leader = resident(1)

    // ---- 代表例 ----

    @Test fun `探索中は進捗と終了時刻と隊長を持つExploringになる`() {
        val state = mapNotificationState(session(), leader, t0.plusSeconds(25))

        assertTrue(state is NotifUiState.Exploring)
        state as NotifUiState.Exploring
        assertEquals(t0, state.startedAt)
        assertEquals(t0.plusSeconds(100), state.endAt)
        assertEquals(0.25, state.progress, 1e-9)
        assertEquals(leader, state.leader)
    }

    @Test fun `帰還時刻を過ぎたらReturnedになる`() {
        val state = mapNotificationState(session(), leader, t0.plusSeconds(100))

        assertEquals(NotifUiState.Returned, state)
    }

    // ---- バリエーション ----

    @Test fun `セッションがなければ通知はない`() {
        assertNull(mapNotificationState(null, leader, t0))
    }

    @Test fun `回収済みセッションなら通知はない`() {
        assertNull(mapNotificationState(session(collected = true), leader, t0.plusSeconds(200)))
    }

    @Test fun `編成が空なら隊長はnullのままExploringになる`() {
        val state = mapNotificationState(session(), leader = null, now = t0.plusSeconds(50))

        assertTrue(state is NotifUiState.Exploring)
        assertNull((state as NotifUiState.Exploring).leader)
    }

    // ---- 境界 ----

    @Test fun `進捗は開始前なら0に、終了直前でも1未満に収まる`() {
        val beforeStart = mapNotificationState(session(), leader, t0.minusSeconds(50))
        assertEquals(0.0, (beforeStart as NotifUiState.Exploring).progress, 1e-9)

        val nearEnd = mapNotificationState(session(), leader, t0.plusSeconds(99))
        assertEquals(0.99, (nearEnd as NotifUiState.Exploring).progress, 1e-9)
    }

    @Test fun `時計を戻すとReturnedからExploringに戻る`() {
        val session = session()

        assertEquals(NotifUiState.Returned, mapNotificationState(session, leader, t0.plusSeconds(100)))

        val rewound = mapNotificationState(session, leader, t0.plusSeconds(50))
        assertTrue(rewound is NotifUiState.Exploring)
        assertEquals(0.5, (rewound as NotifUiState.Exploring).progress, 1e-9)
    }

    private fun session(collected: Boolean = false): ExplorationSession = ExplorationSession(
        id = 1L,
        startedAt = t0,
        endAt = t0.plusSeconds(100),
        seed = 42L,
        partyIds = listOf(1L),
        snapshot = ContextSnapshot(
            date = LocalDate.of(2026, 10, 25),
            timeBand = TimeBand.DAY,
            weather = WeatherKind.UNKNOWN,
        ),
        collected = collected,
    )

    private fun resident(id: Long): Resident = Resident(
        id = id,
        motifId = 1,
        star = 3,
        personalityId = 0,
        colorTier = ColorTier.NORMAL,
        acquiredAt = t0,
        sourceSessionId = 1L,
        locked = false,
    )
}
