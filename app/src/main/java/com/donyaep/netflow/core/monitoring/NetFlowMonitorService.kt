package com.donyaep.netflow.core.monitoring

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.pm.PackageManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
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
    // Un solo hilo: el bucle de muestreo, el colector de ajustes y el reinicio de contadores
    // comparten campos mutables sin sincronizar y no deben ejecutarse a la vez.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
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
    private val accountant = TrafficAccountant(speedBufferSize = SPEED_BUFFER_SIZE)
    private var todayDownloadBytes: Long = 0L
    private var todayUploadBytes: Long = 0L
    private var todayWifiTotalBytes: Long = 0L
    private var todayMobileTotalBytes: Long = 0L
    private val sharedPrefs by lazy { getSharedPreferences("netflow_prefs", Context.MODE_PRIVATE) }
    private var lastDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    private var dataLimitAlertCycleKey: String = ""
    // Ciclo al que corresponde cycleMobileBytes; vacío obliga a consultar de nuevo.
    private var cycleMobileKey: String = ""
    private var cycleMobileBytes: Long = 0L
    // La escribe el receptor en el hilo principal y la lee el bucle en el suyo.
    @Volatile
    private var screenOn = true
    private var lastNotificationKey: String? = null
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            screenOn = intent.action == Intent.ACTION_SCREEN_ON
            // Al encender la pantalla se publica lo que cambió mientras estuvo apagada.
            // Sin monitoreo en marcha no hay notificación que actualizar.
            if (screenOn && monitoringJob != null) serviceScope.launch { updateNotification() }
        }
    }
    private var lastKnownLimitBytes: Long = -1L

    override fun onCreate() {
        super.onCreate()
        NetFlowNotificationFactory.ensureChannel(this)
        screenOn = (getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
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
        unregisterReceiver(screenReceiver)
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

        val initialNetworkType = currentNetworkType()
        accountant.start(trafficStatsRepository.readSnapshot(), SystemClock.elapsedRealtime(), initialNetworkType)
        val currentWifiSsid = currentWifiSsid(initialNetworkType)
        MonitoringStateStore.update {
            it.copy(
                isRunning = true,
                networkType = initialNetworkType,
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
        val networkType = currentNetworkType()

        val currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        if (currentDate != lastDate) {
            lastDate = currentDate
            val newDayUsage = dailyUsageRepository.getTodayUsage()
            todayDownloadBytes = newDayUsage.totalReceivedBytes
            todayUploadBytes = newDayUsage.totalSentBytes
            todayWifiTotalBytes = newDayUsage.wifiTotalBytes
            todayMobileTotalBytes = newDayUsage.mobileTotalBytes
            accountant.rebase(snapshot, nowElapsedRealtime)
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
        val sample = accountant.onSample(snapshot, nowElapsedRealtime, networkType)

        todayDownloadBytes += sample.totalRxDelta
        todayUploadBytes += sample.totalTxDelta
        todayWifiTotalBytes += sample.wifiRxDelta + sample.wifiTxDelta
        todayMobileTotalBytes += sample.mobileRxDelta + sample.mobileTxDelta

        dailyUsageRepository.recordUsage(
            wifiReceivedDelta = sample.wifiRxDelta,
            wifiSentDelta = sample.wifiTxDelta,
            mobileReceivedDelta = sample.mobileRxDelta,
            mobileSentDelta = sample.mobileTxDelta,
        )
        checkDataLimitAlert(sample.mobileRxDelta + sample.mobileTxDelta)

        MonitoringStateStore.update {
            it.copy(
                isRunning = true,
                downloadSpeedBytesPerSecond = sample.downloadSpeed,
                uploadSpeedBytesPerSecond = sample.uploadSpeed,
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
        // transportInfo no existe antes de Android 10; ahí solo queda el fallback de abajo.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            val wifiInfo = (capabilities?.transportInfo) as? WifiInfo
            val primarySsid = wifiInfo?.ssid
            if (primarySsid != null) {
                val cleaned = primarySsid.replace("\"", "").trim()
                if (cleaned.isNotBlank() && !cleaned.equals("<unknown ssid>", ignoreCase = true)) {
                    return primarySsid
                }
            }
        }

        @Suppress("DEPRECATION")
        return wifiManager.connectionInfo?.ssid
    }

    private fun clearRuntimeState() {
        accountant.clear()
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
            cycleMobileKey = ""
            val networkTypeNow = currentNetworkType()
            accountant.start(trafficStatsRepository.readSnapshot(), SystemClock.elapsedRealtime(), networkTypeNow)
            MonitoringStateStore.update {
                it.copy(
                    isRunning = monitoringJob != null,
                    downloadSpeedBytesPerSecond = 0,
                    uploadSpeedBytesPerSecond = 0,
                    networkType = networkTypeNow,
                    wifiSsid = currentWifiSsid(networkTypeNow),
                    todayDownloadBytes = 0,
                    todayUploadBytes = 0,
                    todayWifiTotalBytes = 0,
                    todayMobileTotalBytes = 0,
                )
            }
        }
    }

    private fun updateNotification() {
        // Con la pantalla apagada nadie ve la notificación; se pone al día al encenderla.
        if (!screenOn) return
        val state = MonitoringStateStore.state.value
        val key = NetFlowNotificationFactory.contentKey(state, currentSettings)
        if (key == lastNotificationKey) return
        lastNotificationKey = key
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            NetFlowNotificationFactory.notificationId,
            NetFlowNotificationFactory.build(this, state, currentSettings),
        )
    }

    private suspend fun checkDataLimitAlert(mobileDelta: Long) {
        val limitBytes = if (currentSettings.dataLimitEnabled) currentSettings.toLimitBytes() else 0L
        val cycleKey = billingCycleStart(currentSettings.billingCycleDay).format(DateTimeFormatter.ISO_LOCAL_DATE)
        if (limitBytes <= 0L || cycleKey == dataLimitAlertCycleKey) {
            // Sin límite, o con la alerta del ciclo ya enviada, no se lleva la cuenta. Al
            // reactivarse hay que consultar de nuevo, porque se habrán perdido muestras.
            cycleMobileKey = ""
            return
        }
        if (cycleKey != cycleMobileKey) {
            // La consulta ya incluye la muestra que se acaba de registrar.
            val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            cycleMobileBytes = dailyUsageRepository.getMobileBytesBetween(cycleKey, todayStr)
            cycleMobileKey = cycleKey
        } else {
            cycleMobileBytes += mobileDelta
        }
        if (cycleMobileBytes < limitBytes) return
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

    companion object {
        const val ACTION_START = "com.donyaep.netflow.action.START_MONITOR"
        const val ACTION_STOP = "com.donyaep.netflow.action.STOP_MONITOR"
        const val ACTION_RESET_TODAY = "com.donyaep.netflow.action.RESET_TODAY"
        private const val POLL_INTERVAL_MS = 2_000L
        private const val SPEED_BUFFER_SIZE = 3
    }
}