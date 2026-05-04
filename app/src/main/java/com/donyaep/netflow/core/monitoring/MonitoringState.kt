package com.donyaep.netflow.core.monitoring

data class MonitoringState(
    val isRunning: Boolean = false,
    val downloadSpeedBytesPerSecond: Long = 0,
    val uploadSpeedBytesPerSecond: Long = 0,
    val networkType: NetworkType = NetworkType.None,
    val wifiSsid: String? = null,
    val todayDownloadBytes: Long = 0,
    val todayUploadBytes: Long = 0,
    val todayWifiTotalBytes: Long = 0,
    val todayMobileTotalBytes: Long = 0,
)

enum class NetworkType {
    Wifi,
    Mobile,
    None,
}