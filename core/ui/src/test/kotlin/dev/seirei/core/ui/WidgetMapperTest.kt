package dev.seirei.core.ui

import dev.seirei.core.model.ColorTier
import dev.seirei.core.model.ContextSnapshot
import dev.seirei.core.model.ExplorationSession
import dev.seirei.core.model.Resident
import dev.seirei.core.model.SessionPhase
import dev.seirei.core.model.TimeBand
import dev.seirei.core.model.WeatherKind
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Widget の UiState マッパー（設計書01 §6.1・§6.2、設計書04 §2・§3.4）。
 *
 * 入力は全住人リスト（issue #28 の決定）。フェーズごとの振り分けは `session.partyIds` で行う：
 * IDLE は全住人、EXPLORING は留守番組のみ、RETURNED は全住人＋編成メンバーを帰宅組として区別する。
 * タップ先は IDLE・EXPLORING が `app://home`、RETURNED が `app://collect`。
 */
class WidgetMapperTest {
    private val t0: Instant = Instant.parse("2026-10-25T00:00:00Z")
    private val exploringNow: Instant = t0.plusSeconds(3_600)
    private val returnedNow: Instant = t0.plusSeconds(7_200)

    private val party = listOf(resident(1), resident(2), resident(3))
    private val caretakers = listOf(resident(4), resident(5), resident(6), resident(7))
    private val everyone = party + caretakers

    // ---- 代表例 ----

    @Test fun `セッションがなければ全住人をIDLEで表示し、タップ先はhome`() {
        val state = mapWidgetState(session = null, residents = everyone, now = t0)

        assertEquals(SessionPhase.IDLE, state.phase)
        assertEquals(everyone, state.residents)
        assertTrue(state.returnedParty.isEmpty())
        assertEquals(WidgetTapTarget.HOME, state.tapTarget)
        assertEquals("app://home", state.tapTarget.deepLink)
    }

    @Test fun `探索中は留守番組だけを表示し、タップ先はhome`() {
        val state = mapWidgetState(session(), everyone, exploringNow)

        assertEquals(SessionPhase.EXPLORING, state.phase)
        assertEquals(caretakers, state.residents)
        assertTrue(state.returnedParty.isEmpty())
        assertEquals(WidgetTapTarget.HOME, state.tapTarget)
    }

    @Test fun `帰還時は全住人を戻し、編成メンバーを帰宅組として区別し、タップ先はcollect`() {
        val state = mapWidgetState(session(), everyone, returnedNow)

        assertEquals(SessionPhase.RETURNED, state.phase)
        assertEquals(everyone, state.residents)
        assertEquals(party, state.returnedParty)
        assertEquals(WidgetTapTarget.COLLECT, state.tapTarget)
        assertEquals("app://collect", state.tapTarget.deepLink)
    }

    // ---- バリエーション ----

    @Test fun `回収済みセッションなら、時刻が過ぎていてもIDLEで全住人を表示する`() {
        val state = mapWidgetState(session(collected = true), everyone, returnedNow)

        assertEquals(SessionPhase.IDLE, state.phase)
        assertEquals(everyone, state.residents)
        assertTrue(state.returnedParty.isEmpty())
    }

    @Test fun `住人が1体だけで編成に入っていれば、探索中の表示は空になる`() {
        val only = listOf(resident(1))
        val state = mapWidgetState(session(partyIds = listOf(1L)), only, exploringNow)

        assertEquals(SessionPhase.EXPLORING, state.phase)
        assertTrue(state.residents.isEmpty())
    }

    @Test fun `住人がいなくても各フェーズの状態を返す`() {
        assertTrue(mapWidgetState(null, emptyList(), t0).residents.isEmpty())
        assertTrue(mapWidgetState(session(), emptyList(), exploringNow).residents.isEmpty())

        val returned = mapWidgetState(session(), emptyList(), returnedNow)
        assertEquals(SessionPhase.RETURNED, returned.phase)
        assertTrue(returned.residents.isEmpty())
        assertTrue(returned.returnedParty.isEmpty())
    }

    // ---- 境界 ----

    @Test fun `時計を戻すとRETURNEDからEXPLORINGの表示に戻る`() {
        val session = session()

        assertEquals(SessionPhase.RETURNED, mapWidgetState(session, everyone, returnedNow).phase)

        val rewound = mapWidgetState(session, everyone, exploringNow)
        assertEquals(SessionPhase.EXPLORING, rewound.phase)
        assertEquals(caretakers, rewound.residents)
    }

    private fun session(
        partyIds: List<Long> = listOf(1L, 2L, 3L),
        collected: Boolean = false,
    ): ExplorationSession = ExplorationSession(
        id = 1L,
        startedAt = t0,
        endAt = t0.plusSeconds(7_200),
        seed = 42L,
        partyIds = partyIds,
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
