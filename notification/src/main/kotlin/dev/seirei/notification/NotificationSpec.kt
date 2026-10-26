package dev.seirei.notification

import dev.seirei.core.model.ColorTier
import dev.seirei.core.ui.NotifUiState

/** 通知チャネル（設計書04 §4.4）。文言はユーザーが設定で見る名前そのもの。 */
object NotificationChannels {
    const val EXPLORATION_ID = "exploration_watch"
    const val EXPLORATION_NAME = "探索の見守り"
    const val RETURN_ID = "return_notice"
    const val RETURN_NAME = "帰還のお知らせ"
}

/** 通知で使うアイコンの指定。実 drawable への写像は SeireiNotifications が Assets 経由で行う。 */
enum class NotificationIcon {
    LANTERN_SILHOUETTE,
    GATE_START,
    HOME_END,
}

/** ProgressStyle のトラッカー（設計書01 §6.3）。隊長がいなければランタン。 */
sealed interface TrackerSpec {
    data class ResidentSprite(val motifId: Int, val colorTier: ColorTier) : TrackerSpec
    data object Lantern : TrackerSpec
}

/**
 * 通知1件の純データ（設計書04 §4.1）。Android の型を持たないので JVM テストで検証できる。
 * [progress] が null でないとき ProgressStyle で描く（1.0 は終端＝帰還）。
 */
data class NotificationSpec(
    val channelId: String,
    val title: String,
    val smallIcon: NotificationIcon,
    val startIcon: NotificationIcon?,
    val endIcon: NotificationIcon?,
    val tracker: TrackerSpec?,
    val progress: Double?,
    val whenMillis: Long?,
    val ongoing: Boolean,
    val requestPromoted: Boolean,
    val deepLink: String?,
)

/**
 * NotifUiState から NotificationSpec を作る（設計書04 §4.1・§4.4）。
 * IDLE は [state] が null で、通知自体を出さない。
 */
fun notificationSpec(state: NotifUiState?): NotificationSpec? = when (state) {
    null -> null
    is NotifUiState.Exploring -> NotificationSpec(
        channelId = NotificationChannels.EXPLORATION_ID,
        title = "探索中",
        smallIcon = NotificationIcon.LANTERN_SILHOUETTE,
        startIcon = NotificationIcon.GATE_START,
        endIcon = NotificationIcon.HOME_END,
        tracker = state.leader?.let { TrackerSpec.ResidentSprite(it.motifId, it.colorTier) }
            ?: TrackerSpec.Lantern,
        progress = state.progress,
        whenMillis = state.endAt.toEpochMilli(),
        ongoing = true,
        requestPromoted = true,
        deepLink = "app://home",
    )

    NotifUiState.Returned -> NotificationSpec(
        channelId = NotificationChannels.RETURN_ID,
        title = "精霊が帰ってきた",
        smallIcon = NotificationIcon.LANTERN_SILHOUETTE,
        startIcon = NotificationIcon.GATE_START,
        endIcon = NotificationIcon.HOME_END,
        tracker = null,
        progress = 1.0,
        whenMillis = null,
        ongoing = false,
        requestPromoted = false,
        deepLink = "app://collect",
    )
}
