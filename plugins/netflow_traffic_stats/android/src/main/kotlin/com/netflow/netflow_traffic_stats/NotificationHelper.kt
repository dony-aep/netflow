package com.netflow.netflow_traffic_stats

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlin.math.abs

/**
 * Helper para actualizar la notificación con icono dinámico de velocidad.
 */
object NotificationHelper {
    private const val CHANNEL_PUBLIC = "netflow_monitor"
    private const val CHANNEL_PRIVATE = "netflow_monitor_private"
    private const val CHANNEL_ALERT = "netflow_data_limit"
    private const val CHANNEL_NAME = "NetFlow Monitor"
    private const val CHANNEL_ALERT_NAME = "Alerta de límite de datos"
    private const val CHANNEL_DESCRIPTION = "Monitoreo de uso de datos en tiempo real"
    private const val CHANNEL_ALERT_DESCRIPTION = "Notificación cuando se alcanza el límite de datos configurado"
    private const val NOTIFICATION_ID = 1000
    private const val ALERT_NOTIFICATION_ID = 1001

    private var activeChannelId: String = CHANNEL_PUBLIC
    private var lockscreenVisibility: Int = Notification.VISIBILITY_PUBLIC
    private var lastIconSpeed: Long = -1
    private var lastIsDownload: Boolean = true

    fun createNotificationChannel(context: Context, hideOnLockscreen: Boolean = false) {
        activeChannelId = if (hideOnLockscreen) CHANNEL_PRIVATE else CHANNEL_PUBLIC
        lockscreenVisibility = if (hideOnLockscreen) {
            Notification.VISIBILITY_SECRET
        } else {
            Notification.VISIBILITY_PUBLIC
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(activeChannelId, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
                lockscreenVisibility = this@NotificationHelper.lockscreenVisibility
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ALERT,
                CHANNEL_ALERT_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_ALERT_DESCRIPTION
                setShowBadge(true)
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    fun updateNotification(
        context: Context,
        downloadSpeed: Long,
        uploadSpeed: Long,
        title: String,
        text: String
    ) {
        val isDownload = downloadSpeed >= uploadSpeed
        val primarySpeed = if (isDownload) downloadSpeed else uploadSpeed

        val shouldUpdateIcon = lastIconSpeed < 0 ||
            lastIsDownload != isDownload ||
            abs(primarySpeed - lastIconSpeed) > lastIconSpeed * 0.1

        if (!shouldUpdateIcon && lastIconSpeed >= 0) {
            updateNotificationTextOnly(context, title, text)
            return
        }

        lastIconSpeed = primarySpeed
        lastIsDownload = isDownload

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = baseBuilder(context, title, text)
            .setSmallIcon(context.applicationInfo.icon) // fallback

        val icon = SpeedIconGenerator.generateSpeedIcon(primarySpeed)
        builder.setSmallIcon(icon)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun updateNotificationTextOnly(context: Context, title: String, text: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = baseBuilder(context, title, text)
            .setSmallIcon(context.applicationInfo.icon)

        if (lastIconSpeed >= 0) {
            val icon = SpeedIconGenerator.generateSpeedIcon(lastIconSpeed)
            builder.setSmallIcon(icon)
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun cancelNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        lastIconSpeed = -1
    }

    private fun baseBuilder(context: Context, title: String, text: String): NotificationCompat.Builder {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        } ?: Intent().apply {
            setPackage(context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, activeChannelId)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setVisibility(lockscreenVisibility)
    }

    fun showDataLimitAlert(context: Context, title: String, text: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        } ?: Intent().apply {
            setPackage(context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Usar ic_stat_netflow para el icono de la barra de estado (monocromático)
        val iconResId = context.resources.getIdentifier("ic_stat_netflow", "drawable", context.packageName)
        val smallIcon = if (iconResId != 0) iconResId else context.applicationInfo.icon

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(smallIcon)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationManager.notify(ALERT_NOTIFICATION_ID, builder.build())
    }
}
