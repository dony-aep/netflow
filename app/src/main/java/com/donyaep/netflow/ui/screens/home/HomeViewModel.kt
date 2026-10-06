package com.donyaep.netflow.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.donyaep.netflow.NetFlowApplication
import com.donyaep.netflow.core.monitoring.MonitoringState
import com.donyaep.netflow.core.monitoring.MonitoringStateStore
import com.donyaep.netflow.core.monitoring.NetworkType
import com.donyaep.netflow.core.monitoring.TrafficFormatter
import com.donyaep.netflow.core.monitoring.billingCycleStart
import com.donyaep.netflow.data.model.AppSettings
import com.donyaep.netflow.data.model.DailyUsage
import com.donyaep.netflow.data.model.DataLimitUnit
import com.donyaep.netflow.data.model.SpeedUnit
import com.donyaep.netflow.data.repository.DailyUsageRepository
import com.donyaep.netflow.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val appContainer = (application as NetFlowApplication).appContainer
    private val dailyUsageRepository: DailyUsageRepository = appContainer.dailyUsageRepository
    private val settingsRepository: SettingsRepository = appContainer.settingsRepository
    private var currentSettings: AppSettings = AppSettings()
    private var currentDailyUsage: DailyUsage = DailyUsage.empty(todayDate())
    private var cycleMobileBeforeToday: Long = 0L

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            MonitoringStateStore.state.collect { monitoringState ->
                _uiState.updateFrom(monitoringState, currentSettings, currentDailyUsage, cycleMobileBeforeToday)
            }
        }
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                currentSettings = settings
                refreshCycleBase(settings)
                _uiState.updateFrom(MonitoringStateStore.state.value, currentSettings, currentDailyUsage, cycleMobileBeforeToday)
            }
        }
        viewModelScope.launch {
            dailyUsageRepository.observeTodayUsage().collect { dailyUsage ->
                currentDailyUsage = dailyUsage
                _uiState.updateFrom(MonitoringStateStore.state.value, currentSettings, currentDailyUsage, cycleMobileBeforeToday)
            }
        }
    }

    // Lo de hoy llega en vivo por currentDailyUsage, así que la consulta solo cubre los
    // días anteriores del ciclo y se repite únicamente cuando cambian los ajustes.
    private suspend fun refreshCycleBase(settings: AppSettings) {
        val today = LocalDate.now()
        val cycleStart = billingCycleStart(settings.billingCycleDay, today)
        cycleMobileBeforeToday = if (settings.dataLimitEnabled && cycleStart.isBefore(today)) {
            dailyUsageRepository.getMobileBytesBetween(
                cycleStart.format(DateTimeFormatter.ISO_LOCAL_DATE),
                today.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
            )
        } else {
            0L
        }
    }

    fun resetTodayUsage() {
        viewModelScope.launch {
            dailyUsageRepository.resetTodayUsage()
        }
    }
}

// ─── Private helpers ──────────────────────────────────────────────────────────

private fun MutableStateFlow<HomeUiState>.updateFrom(
    monitoringState: MonitoringState,
    settings: AppSettings,
    dailyUsage: DailyUsage,
    cycleMobileBeforeToday: Long,
) {
    val cycleMobileBytes = cycleMobileBeforeToday + dailyUsage.mobileTotalBytes
    val useBits = settings.speedUnit == SpeedUnit.BitsPerSecond
    val (dlValue, dlUnit) = monitoringState.downloadSpeedBytesPerSecond.toSpeedParts(useBits)
    val (ulValue, ulUnit) = monitoringState.uploadSpeedBytesPerSecond.toSpeedParts(useBits)

    update { _ ->
        HomeUiState(
            isMonitoring = monitoringState.isRunning,
            networkType = monitoringState.networkType,
            connectionLabel = monitoringState.resolveConnectionLabel(),
            downloadSpeedValue = dlValue,
            downloadSpeedUnit = dlUnit,
            uploadSpeedValue = ulValue,
            uploadSpeedUnit = ulUnit,
            downloadBytesPerSecond = monitoringState.downloadSpeedBytesPerSecond,
            uploadBytesPerSecond = monitoringState.uploadSpeedBytesPerSecond,
            todayDownloadLabel = TrafficFormatter.formatBytes(dailyUsage.totalReceivedBytes),
            todayUploadLabel = TrafficFormatter.formatBytes(dailyUsage.totalSentBytes),
            todayTotalLabel = TrafficFormatter.formatBytes(dailyUsage.totalBytes),
            todayWifiLabel = TrafficFormatter.formatBytes(dailyUsage.wifiTotalBytes),
            todayMobileLabel = TrafficFormatter.formatBytes(dailyUsage.mobileTotalBytes),
            todayDownloadBytes = dailyUsage.totalReceivedBytes,
            todayUploadBytes = dailyUsage.totalSentBytes,
            todayTotalBytes = dailyUsage.totalBytes,
            todayWifiBytes = dailyUsage.wifiTotalBytes,
            todayMobileBytes = dailyUsage.mobileTotalBytes,
            dataLimitEnabled = settings.dataLimitEnabled,
            dataLimitBytes = settings.toLimitBytes(),
            dataLimitSummary = settings.toDataLimitSummary(),
            cycleMobileBytes = cycleMobileBytes,
            cycleMobileLabel = TrafficFormatter.formatBytes(cycleMobileBytes),
        )
    }
}

/** Splits "1.4 MB/s" → Pair("1.4", "MB/s"), "150 B/s" → Pair("150", "B/s"). */
private fun Long.toSpeedParts(useBits: Boolean): Pair<String, String> {
    val formatted = TrafficFormatter.formatSpeed(this, useBits)
    val lastSpace = formatted.lastIndexOf(' ')
    return if (lastSpace >= 0) {
        formatted.substring(0, lastSpace) to formatted.substring(lastSpace + 1)
    } else {
        formatted to ""
    }
}

private fun AppSettings.toLimitBytes(): Long {
    if (!dataLimitEnabled) return 0L
    val mult = when (dataLimitUnit) {
        DataLimitUnit.KB -> 1_024L
        DataLimitUnit.MB -> 1_024L * 1_024L
        DataLimitUnit.GB -> 1_024L * 1_024L * 1_024L
    }
    return (dataLimitValue * mult).toLong()
}

private fun AppSettings.toDataLimitSummary(): String {
    if (!dataLimitEnabled) return "Sin límite configurado"
    val valStr = if (dataLimitValue == dataLimitValue.toLong().toDouble()) {
        dataLimitValue.toLong().toString()
    } else {
        "%.1f".format(dataLimitValue)
    }
    return "$valStr ${dataLimitUnit.name} · día $billingCycleDay"
}

private fun todayDate(): String =
    LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

private fun MonitoringState.resolveConnectionLabel(): String = when {
    networkType == NetworkType.Wifi ->
        wifiSsid.takeUnless { it.isNullOrBlank() }?.let { "Conectado a $it" } ?: "Conectado por WiFi"
    networkType == NetworkType.Mobile -> "Conectado por datos móviles"
    else -> "Sin WiFi ni datos móviles"
}
