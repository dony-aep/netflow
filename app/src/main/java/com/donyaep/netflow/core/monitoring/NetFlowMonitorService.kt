package com.donyaep.netflow.core.monitoring

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.app.NotificationManager
import android.app.Service
import android.content.pm.PackageManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.donyaep.netflow.NetFlowApplication
import com.donyaep.netflow.core.notification.NetFlowNotificationFactory
import com.donyaep.netflow.data.model.AppSettings
import com.donyaep.netflow.data.model.TrafficSnapshot
import com.donyaep.netflow.data.repository.DailyUsageRepository
import com.donyaep.netflow.data.repository.SettingsRepository
import com.donyaep.netflow.data.repository.TrafficStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.donyaep.netflow.data.model.DataLimitUnit
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NetFlowMonitorService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val trafficStatsRepository: TrafficStatsRepository by lazy {
        (application as NetFlowApplication).appContainer.trafficStatsRepository
    }
    private val dailyUsageRepository: DailyUsageRepository by lazy {
        (application as NetFlowApplication).appContainer.dailyUsageRepository
    }
    private val settingsRepository: SettingsRepository by lazy {
        (application as NetFlowApplication).appContainer.settingsRepository
    }
    private val connectivityManager: ConnectivityManager by lazy {
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    private val wifiManager: WifiManager by lazy {
        applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    private var monitoringJob: Job? = null
    private var settingsJob: Job? = null
    private var currentSettings: AppSettings = AppSettings()
    private val downloadSpeedBuffer = ArrayDeque<Long>()
    private val uploadSpeedBuffer = ArrayDeque<Long>()
    private var lastSnapshot: TrafficSnapshot? = null
    private var lastSampleElapsedRealtime: Long = 0L
    private var lastNetworkType: NetworkType = NetworkType.None
    private var todayDownloadBytes: Long = 0L
    private var todayUploadBytes: Long = 0L
    private var todayWifiTotalBytes: Long = 0L
    private var todayMobileTotalBytes: Long = 0L
    private val sharedPrefs by lazy { getSharedPreferences("netflow_prefs", Context.MODE_PRIVATE) }
    private var lastDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    private var dataLimitAlertCycleKey: String = ""
    private var lastKnownLimitBytes: Long = -1L

    override fun onCreate() {
        super.onCreate()
        NetFlowNotificationFactory.ensureChannel(this)
        dataLimitAlertCycleKey = sharedPrefs.getString("dataLimitNotifiedCycle", "") ?: ""
        settingsJob = serviceScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                val newLimitBytes = settings.toLimitBytes()
                // Si el límite subió respecto al último conocido, resetear para permitir nueva alerta
                if (newLimitBytes > lastKnownLimitBytes && lastKnownLimitBytes >= 0) {
                    dataLimitAlertCycleKey = ""
                    sharedPrefs.edit().remove("dataLimitNotifiedCycle").apply()
                }
                lastKnownLimitBytes = newLimitBytes
                currentSettings = settings
                if (monitoringJob != null) {
                    updateNotification()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopMonitoring()
            ACTION_RESET_TODAY -> resetTodayStats()
            ACTION_START, null -> startMonitoring()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        monitoringJob?.cancel()
        settingsJob?.cancel()
        MonitoringStateStore.reset()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startMonitoring() {
        if (monitoringJob != null) {
            return
        }

        val initialSnapshot = trafficStatsRepository.readSnapshot()
        lastSnapshot = initialSnapshot
        lastSampleElapsedRealtime = SystemClock.elapsedRealtime()
        lastNetworkType = currentNetworkType()
        val currentWifiSsid = currentWifiSsid(lastNetworkType)
        MonitoringStateStore.update {
            it.copy(
                isRunning = true,
                networkType = lastNetworkType,
                wifiSsid = currentWifiSsid,
            )
        }
        startForeground(
            NetFlowNotificationFactory.notificationId,
            NetFlowNotificationFactory.build(this, MonitoringStateStore.state.value, currentSettings),
        )

        monitoringJob = serviceScope.launch {
            val todayUsage = dailyUsageRepository.getTodayUsage()
            todayDownloadBytes = todayUsage.totalReceivedBytes
            todayUploadBytes = todayUsage.totalSentBytes
            todayWifiTotalBytes = todayUsage.wifiTotalBytes
            todayMobileTotalBytes = todayUsage.mobileTotalBytes
            MonitoringStateStore.update {
                it.copy(
                    todayDownloadBytes = todayDownloadBytes,
                    todayUploadBytes = todayUploadBytes,
                    todayWifiTotalBytes = todayWifiTotalBytes,
                    todayMobileTotalBytes = todayMobileTotalBytes,
                )
            }
            updateNotification()

            while (isActive) {
                val snapshot = trafficStatsRepository.readSnapshot()
                publishMonitoringState(snapshot)
                updateNotification()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        clearRuntimeState()
        MonitoringStateStore.reset()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun publishMonitoringState(snapshot: TrafficSnapshot) {
        val nowElapsedRealtime = SystemClock.elapsedRealtime()
        val elapsedMs = nowElapsedRealtime - lastSampleElapsedRealtime
        val networkType = currentNetworkType()
        val previousSnapshot = lastSnapshot
        val networkChanged = networkType != lastNetworkType

        val currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        if (currentDate != lastDate) {
            lastDate = currentDate
            val newDayUsage = dailyUsageRepository.getTodayUsage()
            todayDownloadBytes = newDayUsage.totalReceivedBytes
            todayUploadBytes = newDayUsage.totalSentBytes
            todayWifiTotalBytes = newDayUsage.wifiTotalBytes
            todayMobileTotalBytes = newDayUsage.mobileTotalBytes
            lastSnapshot = snapshot
            lastSampleElapsedRealtime = nowElapsedRealtime
            downloadSpeedBuffer.clear()
            uploadSpeedBuffer.clear()
            MonitoringStateStore.update {
                it.copy(
                    todayDownloadBytes = todayDownloadBytes,
                    todayUploadBytes = todayUploadBytes,
                    todayWifiTotalBytes = todayWifiTotalBytes,
                    todayMobileTotalBytes = todayMobileTotalBytes,
                    downloadSpeedBytesPerSecond = 0,
                    uploadSpeedBytesPerSecond = 0,
                )
            }
            return
        }
        val wifiSsid = currentWifiSsid(networkType)
        var wifiRxDelta = 0L
        var wifiTxDelta = 0L
        var mobileRxDelta = 0L
        var mobileTxDelta = 0L

        var averagedDownloadSpeed = 0L
        var averagedUploadSpeed = 0L

        if (previousSnapshot != null && elapsedMs > 100L) {
            // Siempre contar bytes — independientemente del estado de red
            wifiRxDelta = (snapshot.wifiRxBytes - previousSnapshot.wifiRxBytes).coerceAtLeast(0L)
            wifiTxDelta = (snapshot.wifiTxBytes - previousSnapshot.wifiTxBytes).coerceAtLeast(0L)
            mobileRxDelta = (snapshot.mobileRxBytes - previousSnapshot.mobileRxBytes).coerceAtLeast(0L)
            mobileTxDelta = (snapshot.mobileTxBytes - previousSnapshot.mobileTxBytes).coerceAtLeast(0L)

            todayDownloadBytes += wifiRxDelta + mobileRxDelta
            todayUploadBytes += wifiTxDelta + mobileTxDelta
            todayWifiTotalBytes += wifiRxDelta + wifiTxDelta
            todayMobileTotalBytes += mobileRxDelta + mobileTxDelta

            dailyUsageRepository.recordUsage(
                wifiReceivedDelta = wifiRxDelta,
                wifiSentDelta = wifiTxDelta,
                mobileReceivedDelta = mobileRxDelta,
                mobileSentDelta = mobileTxDelta,
            )
            checkDataLimitAlert()

            // Velocidad solo cuando la red es estable (sin cambio y con conexión activa)
            if (!networkChanged && networkType != NetworkType.None) {
                val rxDelta = (snapshot.totalRxBytes - previousSnapshot.totalRxBytes).coerceAtLeast(0L)
                val txDelta = (snapshot.totalTxBytes - previousSnapshot.totalTxBytes).coerceAtLeast(0L)
                val rawDownloadSpeed = (rxDelta * 1000L) / elapsedMs
                val rawUploadSpeed = (txDelta * 1000L) / elapsedMs

                pushBufferedSample(downloadSpeedBuffer, rawDownloadSpeed)
                pushBufferedSample(uploadSpeedBuffer, rawUploadSpeed)

                averagedDownloadSpeed = downloadSpeedBuffer.averageBytesPerSecond()
                averagedUploadSpeed = uploadSpeedBuffer.averageBytesPerSecond()
            } else {
                downloadSpeedBuffer.clear()
                uploadSpeedBuffer.clear()
            }
        } else {
            downloadSpeedBuffer.clear()
            uploadSpeedBuffer.clear()
        }

        lastSnapshot = snapshot
        lastSampleElapsedRealtime = nowElapsedRealtime
        lastNetworkType = networkType

        MonitoringStateStore.update {
            it.copy(
                isRunning = true,
                downloadSpeedBytesPerSecond = averagedDownloadSpeed,
                uploadSpeedBytesPerSecond = averagedUploadSpeed,
                networkType = networkType,
                wifiSsid = wifiSsid,
                todayDownloadBytes = todayDownloadBytes,
                todayUploadBytes = todayUploadBytes,
                todayWifiTotalBytes = todayWifiTotalBytes,
                todayMobileTotalBytes = todayMobileTotalBytes,
            )
        }
    }

    private fun currentNetworkType(): NetworkType {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            ?: return NetworkType.None

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.Wifi
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.Mobile
            else -> NetworkType.None
        }
    }

    private fun currentWifiSsid(networkType: NetworkType): String? {
        if (networkType != NetworkType.Wifi) {
            return null
        }

        val hasLocationPermission =
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasLocationPermission) {
            return null
        }

        val ssid = readWifiInfoSsid() ?: return null
        val sanitized = ssid.replace("\"", "").trim()
        return sanitized.takeUnless {
            it.isBlank() || it.equals("<unknown ssid>", ignoreCase = true)
        }
    }

    private fun readWifiInfoSsid(): String? {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val wifiInfo = (capabilities?.transportInfo) as? WifiInfo
        val primarySsid = wifiInfo?.ssid
        if (primarySsid != null) {
            val cleaned = primarySsid.replace("\"", "").trim()
            if (cleaned.isNotBlank() && !cleaned.equals("<unknown ssid>", ignoreCase = true)) {
                return primarySsid
            }
        }

        @Suppress("DEPRECATION")
        return wifiManager.connectionInfo?.ssid
    }

    private fun pushBufferedSample(buffer: ArrayDeque<Long>, value: Long) {
        buffer.addLast(value)
        if (buffer.size > SPEED_BUFFER_SIZE) {
            buffer.removeFirst()
        }
    }

    private fun ArrayDeque<Long>.averageBytesPerSecond(): Long {
        if (isEmpty()) {
            return 0L
        }
        return sum() / size
    }

    private fun clearRuntimeState() {
        downloadSpeedBuffer.clear()
        uploadSpeedBuffer.clear()
        lastSnapshot = null
        lastSampleElapsedRealtime = 0L
        lastNetworkType = NetworkType.None
        todayDownloadBytes = 0L
        todayUploadBytes = 0L
        todayWifiTotalBytes = 0L
        todayMobileTotalBytes = 0L
        lastDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    private fun resetTodayStats() {
        serviceScope.launch {
            dailyUsageRepository.resetTodayUsage()
            clearRuntimeState()
            lastSnapshot = trafficStatsRepository.readSnapshot()
            lastSampleElapsedRealtime = SystemClock.elapsedRealtime()
            lastNetworkType = currentNetworkType()
            MonitoringStateStore.update {
                it.copy(
                    isRunning = monitoringJob != null,
                    downloadSpeedBytesPerSecond = 0,
                    uploadSpeedBytesPerSecond = 0,
                    networkType = lastNetworkType,
                    wifiSsid = currentWifiSsid(lastNetworkType),
                    todayDownloadBytes = 0,
                    todayUploadBytes = 0,
                    todayWifiTotalBytes = 0,
                    todayMobileTotalBytes = 0,
                )
            }
        }
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            NetFlowNotificationFactory.notificationId,
            NetFlowNotificationFactory.build(this, MonitoringStateStore.state.value, currentSettings),
        )
    }

    private suspend fun checkDataLimitAlert() {
        if (!currentSettings.dataLimitEnabled) return
        val limitBytes = currentSettings.toLimitBytes()
        if (limitBytes <= 0L) return
        val cycleStart = getCycleStartDate(currentSettings.billingCycleDay)
        val cycleKey = cycleStart.format(DateTimeFormatter.ISO_LOCAL_DATE)
        if (cycleKey == dataLimitAlertCycleKey) return
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val cycleTotalBytes = dailyUsageRepository.getTotalBytesBetween(cycleKey, todayStr)
        if (cycleTotalBytes < limitBytes) return
        dataLimitAlertCycleKey = cycleKey
        sharedPrefs.edit().putString("dataLimitNotifiedCycle", cycleKey).apply()
        NetFlowNotificationFactory.sendDataLimitAlert(this, currentSettings)
    }

    private fun AppSettings.toLimitBytes(): Long {
        val mult = when (dataLimitUnit) {
            DataLimitUnit.KB -> 1_024L
            DataLimitUnit.MB -> 1_024L * 1_024L
            DataLimitUnit.GB -> 1_024L * 1_024L * 1_024L
        }
        return (dataLimitValue * mult).toLong()
    }

    private fun getCycleStartDate(billingCycleDay: Int): LocalDate {
        val today = LocalDate.now()
        val safeDay = billingCycleDay.coerceIn(1, today.lengthOfMonth())
        return if (today.dayOfMonth >= safeDay) {
            today.withDayOfMonth(safeDay)
        } else {
            val prevMonth = today.minusMonths(1)
            prevMonth.withDayOfMonth(billingCycleDay.coerceAtMost(prevMonth.lengthOfMonth()))
        }
    }

    companion object {
        const val ACTION_START = "com.donyaep.netflow.action.START_MONITOR"
        const val ACTION_STOP = "com.donyaep.netflow.action.STOP_MONITOR"
        const val ACTION_RESET_TODAY = "com.donyaep.netflow.action.RESET_TODAY"
        private const val POLL_INTERVAL_MS = 2_000L
        private const val SPEED_BUFFER_SIZE = 3
    }
}