package dev.seirei.core.ui

import dev.seirei.core.model.ExplorationSession
import dev.seirei.core.model.Resident
import dev.seirei.core.model.SessionPhase
import dev.seirei.core.model.derivePhase
import java.time.Duration
import java.time.Instant

/** Widget のタップ先（設計書04 §3.4）。 */
enum class WidgetTapTarget(val deepLink: String) {
    HOME("app://home"),
    COLLECT("app://collect"),
}

/**
 * Widget に表示する状態（設計書01 §6.1・§6.2、設計書04 §2）。
 *
 * [residents] はフェーズで振り分けた表示対象。[returnedParty] は RETURNED のときに
 * 帰宅した編成メンバーで、それ以外のフェーズでは空。
 */
data class WidgetUiState(
    val phase: SessionPhase,
    val residents: List<Resident>,
    val returnedParty: List<Resident>,
    val tapTarget: WidgetTapTarget,
)

/** 通知に表示する状態（設計書01 §6.1、設計書04 §4.1）。IDLE は通知なしなのでマッパーは null を返す。 */
sealed interface NotifUiState {
    /**
     * 探索中。[progress] は 0..1。 [leader] が null のときは編成が空で、
     * 通知側はトラッカーをランタンにフォールバックする（設計書01 §6）。
     */
    data class Exploring(
        val startedAt: Instant,
        val endAt: Instant,
        val progress: Double,
        val leader: Resident?,
    ) : NotifUiState

    /** 帰還済み。「精霊が帰ってきた」通知を出す。 */
    data object Returned : NotifUiState
}

/**
 * 全住人から Widget の状態を導出する（設計書01 §6.1・§6.2）。
 *
 * 振り分けはセッションが点火時に固定した `partyIds` で行う（issue #28 の入力契約）：
 * IDLE は全住人、EXPLORING は partyIds を除いた留守番組のみ、
 * RETURNED は全住人に加えて partyIds の住人を帰宅組として区別する。
 */
fun mapWidgetState(
    session: ExplorationSession?,
    residents: List<Resident>,
    now: Instant,
): WidgetUiState = when (derivePhase(session, now)) {
    SessionPhase.IDLE -> WidgetUiState(
        phase = SessionPhase.IDLE,
        residents = residents,
        returnedParty = emptyList(),
        tapTarget = WidgetTapTarget.HOME,
    )

    SessionPhase.EXPLORING -> {
        val partyIds = liveSession(session).partyIds.toSet()
        WidgetUiState(
            phase = SessionPhase.EXPLORING,
            residents = residents.filter { it.id !in partyIds },
            returnedParty = emptyList(),
            tapTarget = WidgetTapTarget.HOME,
        )
    }

    SessionPhase.RETURNED -> {
        val byId = residents.associateBy { it.id }
        WidgetUiState(
            phase = SessionPhase.RETURNED,
            residents = residents,
            returnedParty = liveSession(session).partyIds.mapNotNull { byId[it] },
            tapTarget = WidgetTapTarget.COLLECT,
        )
    }
}

/**
 * セッションと隊長から通知の状態を導出する（設計書01 §6.1）。
 * 進行度は `(now − startedAt) / (endAt − startedAt)` を 0..1 にクランプする。
 */
fun mapNotificationState(
    session: ExplorationSession?,
    leader: Resident?,
    now: Instant,
): NotifUiState? = when (derivePhase(session, now)) {
    SessionPhase.IDLE -> null
    SessionPhase.RETURNED -> NotifUiState.Returned
    SessionPhase.EXPLORING -> {
        val active = liveSession(session)
        NotifUiState.Exploring(
            startedAt = active.startedAt,
            endAt = active.endAt,
            progress = progress(active, now),
            leader = leader,
        )
    }
}

private fun progress(session: ExplorationSession, now: Instant): Double {
    val total = Duration.between(session.startedAt, session.endAt)
    if (total.isZero || total.isNegative) return 0.0
    val elapsed = Duration.between(session.startedAt, now)
    return (elapsed.toNanos().toDouble() / total.toNanos().toDouble()).coerceIn(0.0, 1.0)
}

/** derivePhase が IDLE でないなら、セッションは存在し未回収である。 */
private fun liveSession(session: ExplorationSession?): ExplorationSession =
    requireNotNull(session) { "A non-IDLE phase requires a session" }
