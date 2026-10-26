package dev.seirei.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import dev.seirei.core.model.ColorTier
import dev.seirei.core.ui.Assets

/**
 * NotificationSpec を Android の通知へ写す薄い層（設計書04 §4.1・§4.4）。
 * 判定はすべて NotificationSpec 側にあり、ここはアイコンと Intent の解決だけを行う。
 */
object SeireiNotifications {

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                NotificationChannels.EXPLORATION_ID,
                NotificationChannels.EXPLORATION_NAME,
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                NotificationChannels.RETURN_ID,
                NotificationChannels.RETURN_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    fun build(context: Context, spec: NotificationSpec): Notification {
        val builder = NotificationCompat.Builder(context, spec.channelId)
            .setSmallIcon(spec.smallIcon.toDrawable())
            .setContentTitle(spec.title)
            .setOngoing(spec.ongoing)
            .setOnlyAlertOnce(true)

        spec.whenMillis?.let { whenMillis ->
            builder.setWhen(whenMillis)
                .setShowWhen(true)
                .setUsesChronometerCountDown(true)
        }
        if (spec.requestPromoted) {
            builder.setRequestPromotedOngoing(true)
        }
        spec.progress?.let { progress ->
            val style = NotificationCompat.ProgressStyle()
                .setProgress((progress * 100).toInt())
            spec.startIcon?.let { style.setProgressStartIcon(iconCompat(context, it)) }
            spec.endIcon?.let { style.setProgressEndIcon(iconCompat(context, it)) }
            spec.tracker?.let { style.setProgressTrackerIcon(trackerIconCompat(context, it)) }
            builder.setStyle(style)
        }
        spec.deepLink?.let { deepLink ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
                .setPackage(context.packageName)
            builder.setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        }
        return builder.build()
    }

    private fun NotificationIcon.toDrawable(): Int = when (this) {
        NotificationIcon.LANTERN_SILHOUETTE -> Assets.icLanternSilhouette
        NotificationIcon.GATE_START -> Assets.icGateStart
        NotificationIcon.HOME_END -> Assets.icHomeEnd
    }

    private fun iconCompat(context: Context, icon: NotificationIcon): IconCompat =
        IconCompat.createWithResource(context, icon.toDrawable())

    private fun trackerIconCompat(context: Context, tracker: TrackerSpec): IconCompat =
        when (tracker) {
            is TrackerSpec.ResidentSprite -> {
                val sprites = when (tracker.colorTier) {
                    ColorTier.NORMAL -> Assets.residentNormal
                    ColorTier.RARE -> Assets.residentRare
                    ColorTier.GLOW -> Assets.residentGlow
                }
                IconCompat.createWithResource(context, sprites[tracker.motifId - 1])
            }

            TrackerSpec.Lantern -> IconCompat.createWithResource(context, Assets.propLantern)
        }
}
