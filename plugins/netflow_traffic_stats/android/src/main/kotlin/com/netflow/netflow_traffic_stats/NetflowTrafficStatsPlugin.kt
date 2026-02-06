package com.netflow.netflow_traffic_stats

import android.content.Context
import android.net.TrafficStats
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.Result

/** NetflowTrafficStatsPlugin */
class NetflowTrafficStatsPlugin : FlutterPlugin {
    private lateinit var appContext: Context
    private lateinit var trafficChannel: MethodChannel
    private lateinit var notificationChannel: MethodChannel

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        appContext = flutterPluginBinding.applicationContext

        trafficChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.netflow.app/traffic_stats")
        trafficChannel.setMethodCallHandler(::handleTrafficMethods)

        notificationChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.netflow.app/notification")
        notificationChannel.setMethodCallHandler(::handleNotificationMethods)
    }

    private fun handleTrafficMethods(
        call: MethodCall,
        result: Result
    ) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }

            "getTrafficStats" -> {
                try {
                    val totalRx = TrafficStats.getTotalRxBytes()
                    val totalTx = TrafficStats.getTotalTxBytes()
                    val mobileRx = TrafficStats.getMobileRxBytes()
                    val mobileTx = TrafficStats.getMobileTxBytes()

                    // WiFi se calcula como total - móvil.
                    val wifiRx = if (totalRx > mobileRx) totalRx - mobileRx else 0L
                    val wifiTx = if (totalTx > mobileTx) totalTx - mobileTx else 0L

                    result.success(
                        mapOf(
                            "totalRx" to totalRx,
                            "totalTx" to totalTx,
                            "mobileRx" to mobileRx,
                            "mobileTx" to mobileTx,
                            "wifiRx" to wifiRx,
                            "wifiTx" to wifiTx,
                        )
                    )
                } catch (e: Exception) {
                    result.error("TRAFFIC_STATS_ERROR", e.message, null)
                }
            }

            else -> result.notImplemented()
        }
    }

    private fun handleNotificationMethods(
        call: MethodCall,
        result: Result
    ) {
        when (call.method) {
            "initChannel" -> {
                try {
                    val hideOnLockscreen = call.argument<Boolean>("hideOnLockscreen") ?: false
                    NotificationHelper.createNotificationChannel(appContext, hideOnLockscreen)
                    result.success(true)
                } catch (e: Exception) {
                    result.error("INIT_ERROR", e.message, null)
                }
            }

            "updateNotification" -> {
                try {
                    val downloadSpeed = call.argument<Number>("downloadSpeed")?.toLong() ?: 0L
                    val uploadSpeed = call.argument<Number>("uploadSpeed")?.toLong() ?: 0L
                    val title = call.argument<String>("title") ?: ""
                    val text = call.argument<String>("text") ?: ""

                    NotificationHelper.updateNotification(
                        appContext,
                        downloadSpeed,
                        uploadSpeed,
                        title,
                        text,
                    )
                    result.success(true)
                } catch (e: Exception) {
                    result.error("UPDATE_ERROR", e.message, null)
                }
            }

            "cancelNotification" -> {
                try {
                    NotificationHelper.cancelNotification(appContext)
                    result.success(true)
                } catch (e: Exception) {
                    result.error("CANCEL_ERROR", e.message, null)
                }
            }

            "showDataLimitAlert" -> {
                try {
                    val title = call.argument<String>("title") ?: ""
                    val text = call.argument<String>("text") ?: ""

                    NotificationHelper.showDataLimitAlert(appContext, title, text)
                    result.success(true)
                } catch (e: Exception) {
                    result.error("ALERT_ERROR", e.message, null)
                }
            }

            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        trafficChannel.setMethodCallHandler(null)
        notificationChannel.setMethodCallHandler(null)
    }
}
