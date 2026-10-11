package dev.seirei.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/**
 * derivePhase（設計書01 §3）と TLA+ の Phase 定義の対応を固定する。
 * 異常系カタログ（設計書01 §7）のうち、純関数で表せるもの（#7 時計を進める・#8 時計を戻す）を含む。
 * ハンドラ側の異常系（#5 連打・#6 古い通知）は :core:domain のテスト（E3-02）で扱う。
 */
class DerivePhaseTest {
    private val t0 = Instant.parse("2026-10-11T00:00:00Z")
    private fun session(endAt: Instant, collected: Boolean = false) = ExplorationSession(
        id = 1, startedAt = t0, endAt = endAt, seed = 42, partyIds = listOf(10L, 11L),
        snapshot = ContextSnapshot(LocalDate.of(2026, 10, 11), TimeBand.DAY, WeatherKind.RAIN),
        collected = collected,
    )

    @Test fun `セッションなしは IDLE`() =
        assertEquals(SessionPhase.IDLE, derivePhase(null, t0))

    @Test fun `endAt より前は EXPLORING`() =
        assertEquals(SessionPhase.EXPLORING, derivePhase(session(t0.plusSeconds(7200)), t0.plusSeconds(7199)))

    @Test fun `endAt ちょうどで RETURNED（境界は now ≥ endAt）`() =
        assertEquals(SessionPhase.RETURNED, derivePhase(session(t0.plusSeconds(7200)), t0.plusSeconds(7200)))

    @Test fun `回収済みは時刻によらず IDLE`() {
        val s = session(t0.plusSeconds(7200), collected = true)
        assertEquals(SessionPhase.IDLE, derivePhase(s, t0))
        assertEquals(SessionPhase.IDLE, derivePhase(s, t0.plusSeconds(99_999)))
    }

    @Test fun `異常系7 時計を進めると RETURNED（チートは受容。安全性は壊れない）`() =
        assertEquals(SessionPhase.RETURNED, derivePhase(session(t0.plusSeconds(7200)), t0.plusSeconds(86_400)))

    @Test fun `異常系8 時計を戻すと RETURNED から EXPLORING へ戻る`() {
        val s = session(t0.plusSeconds(7200))
        assertEquals(SessionPhase.RETURNED, derivePhase(s, t0.plusSeconds(7200)))
        assertEquals(SessionPhase.EXPLORING, derivePhase(s, t0.plusSeconds(3600)))
    }
}
