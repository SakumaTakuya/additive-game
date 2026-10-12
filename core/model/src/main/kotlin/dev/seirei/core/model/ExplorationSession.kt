package dev.seirei.core.model

import java.time.Instant
import java.time.LocalDate

/**
 * 探索セッションの永続レコード（設計書01 §3）。保存するのは不変の事実だけ（原則A）。
 * フェーズ（IDLE / EXPLORING / RETURNED）は保存せず、[derivePhase] で毎回導出する（原則B）。
 */
data class ExplorationSession(
    val id: Long,                  // 単調増加。回収の冪等キー（collection_receipt.sessionId）
    val startedAt: Instant,        // 点火時刻（壁時計）
    val endAt: Instant,            // 点火時に確定（ランタン方式）。以後変えない
    val seed: Long,                // ロール・バトルログのすべてを決めるシード
    val partyIds: List<Long>,      // 編成（住人個体ID）。[0] が隊長
    val snapshot: ContextSnapshot, // 点火時の外部世界の固定値（設計書02 原則E）
    val collected: Boolean,        // 回収済みフラグ
)

/** 点火時に固定する外部世界の値（設計書01 §3、設計書02 §2）。 */
data class ContextSnapshot(
    val date: LocalDate,        // 曜日・曜日祭壇・月齢の導出元（端末タイムゾーンのローカル日付）
    val timeBand: TimeBand,     // 点火時刻の時間帯
    val weather: WeatherKind,   // 点火時点の保存値（設計書03 §2.1 effectiveWeather）。未取得なら UNKNOWN
)

enum class SessionPhase { IDLE, EXPLORING, RETURNED }

/**
 * フェーズ導出（設計書01 §3）。TLA+ の `Phase` 定義（formal/ExplorationSession.tla）と1対1に対応する。
 * この関数を変える場合は、先に TLA+ 仕様を変えて TLC で再検査すること（AGENTS.md）。
 */
fun derivePhase(s: ExplorationSession?, now: Instant): SessionPhase = when {
    s == null || s.collected -> SessionPhase.IDLE
    now < s.endAt -> SessionPhase.EXPLORING
    else -> SessionPhase.RETURNED
}
