package com.donyaep.netflow.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.donyaep.netflow.MainActivity
import com.donyaep.netflow.R
import com.donyaep.netflow.core.monitoring.MonitoringState
import com.donyaep.netflow.core.monitoring.NetFlowMonitorService
import com.donyaep.netflow.core.monitoring.NetworkType
import com.donyaep.netflow.core.monitoring.TrafficFormatter
import com.donyaep.netflow.data.model.AppSettings
import com.donyaep.netflow.data.model.SpeedUnit

object NetFlowNotificationFactory {
    const val channelId = "netflow_monitor"
    const val notificationId = 1000
    const val alertChannelId = "netflow_data_limit"
    const val alertNotificationId = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            channelId,
            "NetFlow Monitor",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Monitoreo de uso de datos en tiempo real"
            setShowBadge(false)
            enableVibration(false)
            enableLights(false)
        }
        val alertChannel = NotificationChannel(
            alertChannelId,
            "Alerta de límite de datos",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notificación cuando se alcanza el límite de datos configurado"
            setShowBadge(true)
            enableVibration(true)
        }

        manager.createNotificationChannel(channel)
        manager.createNotificationChannel(alertChannel)
    }

    fun build(
        context: Context,
        monitoringState: MonitoringState,
        settings: AppSettings,
    ): android.app.Notification {
        val useBits = settings.speedUnit == SpeedUnit.BitsPerSecond
        val title = "Bajada: ${TrafficFormatter.formatSpeed(monitoringState.downloadSpeedBytesPerSecond, useBits)}  Subida: ${TrafficFormatter.formatSpeed(monitoringState.uploadSpeedBytesPerSecond, useBits)}"
        val splitText = buildString {
            append(networkLabel(monitoringState))
            append(" · WiFi ")
            append(TrafficFormatter.formatBytes(monitoringState.todayWifiTotalBytes))
            append(" · Móvil ")
            append(TrafficFormatter.formatBytes(monitoringState.todayMobileTotalBytes))
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, NetFlowMonitorService::class.java).apply {
                action = NetFlowMonitorService.ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(
                IconCompat.createWithBitmap(
                    DynamicSpeedIconFactory.create(
                        context = context,
                        downloadSpeedBytesPerSecond = monitoringState.downloadSpeedBytesPerSecond,
                        uploadSpeedBytesPerSecond = monitoringState.uploadSpeedBytesPerSecond,
                        speedUnit = settings.speedUnit,
                    ),
                ),
            )
            .setContentTitle(title)
            .setContentText(splitText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(splitText))
            .setContentIntent(contentIntent)
            .addAction(0, "Detener", stopIntent)
            .setVisibility(
                if (settings.hideOnLockscreen) NotificationCompat.VISIBILITY_SECRET else NotificationCompat.VISIBILITY_PUBLIC,
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun sendDataLimitAlert(context: Context, settings: AppSettings) {
        val limitText = formatLimit(settings)
        val contentIntent = PendingIntent.getActivity(
            context,
            2,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, alertChannelId)
            .setSmallIcon(R.drawable.ic_stat_netflow)
            .setContentTitle("Límite de datos alcanzado")
            .setContentText("Has superado tu límite de $limitText/mes")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Has superado tu límite mensual de $limitText. Considera revisar tu consumo de datos."),
            )
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(alertNotificationId, notification)
    }

    private fun formatLimit(settings: AppSettings): String {
        val valStr = if (settings.dataLimitValue == settings.dataLimitValue.toLong().toDouble()) {
            settings.dataLimitValue.toLong().toString()
        } else {
            "%.1f".format(settings.dataLimitValue)
        }
        return "$valStr ${settings.dataLimitUnit.name}"
    }

    private fun networkLabel(networkType: NetworkType): String = when (networkType) {
        NetworkType.Wifi -> "WiFi"
        NetworkType.Mobile -> "Datos móviles"
        NetworkType.None -> "Sin conexión"
    }

    private fun networkLabel(monitoringState: MonitoringState): String {
        return if (
            monitoringState.networkType == NetworkType.Wifi &&
            !monitoringState.wifiSsid.isNullOrBlank()
        ) {
            "WiFi: ${monitoringState.wifiSsid}"
        } else {
            networkLabel(monitoringState.networkType)
        }
    }
}