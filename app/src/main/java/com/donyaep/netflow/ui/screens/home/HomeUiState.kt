package com.donyaep.netflow.ui.screens.home

import com.donyaep.netflow.core.monitoring.NetworkType

data class HomeUiState(
    // Monitoring status
    val isMonitoring: Boolean = false,
    val networkType: NetworkType = NetworkType.None,
    val connectionLabel: String = "Sin detalles de conexión",
    // Real-time speed — value and unit split for large display
    val downloadSpeedValue: String = "0",
    val downloadSpeedUnit: String = "B/s",
    val uploadSpeedValue: String = "0",
    val uploadSpeedUnit: String = "B/s",
    // Today usage (human-readable labels)
    val todayDownloadLabel: String = "0 B",
    val todayUploadLabel: String = "0 B",
    val todayTotalLabel: String = "0 B",
    val todayWifiLabel: String = "0 B",
    val todayMobileLabel: String = "0 B",
    // Raw byte counts for progress bar calculations
    val todayDownloadBytes: Long = 0L,
    val todayUploadBytes: Long = 0L,
    val todayTotalBytes: Long = 0L,
    // Data limit info
    val dataLimitEnabled: Boolean = false,
    val dataLimitBytes: Long = 0L,
    val dataLimitSummary: String = "Sin límite configurado",
)
