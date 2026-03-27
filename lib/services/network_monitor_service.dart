import 'dart:async';

import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter/material.dart';
import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'package:network_info_plus/network_info_plus.dart';
import 'package:netflow_traffic_stats/netflow_traffic_stats.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../models/models.dart';
import 'data_formatter.dart';
import 'database_service.dart';
import 'native_notification_service.dart';

/// Callback que se ejecuta cuando se inicia el servicio de foreground.
@pragma('vm:entry-point')
void startCallback() {
  FlutterForegroundTask.setTaskHandler(NetworkMonitorTaskHandler());
}

/// Handler de background: ejecuta polling real en segundo plano.
class NetworkMonitorTaskHandler extends TaskHandler {
  static const int _speedBufferSize = 3;
  static const int _dbSaveIntervalTicks = 30;
  static const Duration _ssidCacheDuration = Duration(seconds: 10);

  final List<int> _downloadSpeedBuffer = [];
  final List<int> _uploadSpeedBuffer = [];

  int _lastRxBytes = 0;
  int _lastTxBytes = 0;
  int _lastWifiRx = 0;
  int _lastWifiTx = 0;
  int _lastMobileRx = 0;
  int _lastMobileTx = 0;

  int _todayRxBytes = 0;
  int _todayTxBytes = 0;
  int _todayWifiRx = 0;
  int _todayWifiTx = 0;
  int _todayMobileRx = 0;
  int _todayMobileTx = 0;

  int _lastSavedWifiRx = 0;
  int _lastSavedWifiTx = 0;
  int _lastSavedMobileRx = 0;
  int _lastSavedMobileTx = 0;

  int _dbTickCounter = 0;
  DateTime _lastUpdate = DateTime.now();
  DateTime _dayStart = DateTime.now();
  NetworkType _lastNetworkType = NetworkType.none;

  String? _cachedWifiSsid;
  DateTime? _lastSsidUpdate;

  bool _useBitsUnit = false;
  bool _useNativeNotification = false;
  bool _isUpdating = false;

  // Configuración de límite de datos
  bool _dataLimitEnabled = false;
  int _dataLimitBytes = 0;
  int _billingCycleDay = 1;
  bool _dataLimitNotified = false;
  String _notifiedCycleKey = '';

  @override
  Future<void> onStart(DateTime timestamp, TaskStarter starter) async {
    final prefs = await SharedPreferences.getInstance();
    _useBitsUnit = prefs.getInt('speedUnit') == 0; // 0 = bits/s
    final hideOnLockscreen = prefs.getBool('hideOnLockscreen') ?? false;
    _useNativeNotification = await NativeNotificationService.init(
      hideOnLockscreen: hideOnLockscreen,
    );

    _dayStart = DateTime(timestamp.year, timestamp.month, timestamp.day);
    _lastUpdate = DateTime.now();

    // Cargar datos ya acumulados del día desde la BD
    try {
      final todayUsage = await DatabaseService.getTodayUsage();
      _todayRxBytes = todayUsage.totalReceived;
      _todayTxBytes = todayUsage.totalSent;
      _todayWifiRx = todayUsage.wifiReceived;
      _todayWifiTx = todayUsage.wifiSent;
      _todayMobileRx = todayUsage.mobileReceived;
      _todayMobileTx = todayUsage.mobileSent;

      // Sincronizar _lastSaved* para no re-guardar lo que ya existe en la BD
      _lastSavedWifiRx = _todayWifiRx;
      _lastSavedWifiTx = _todayWifiTx;
      _lastSavedMobileRx = _todayMobileRx;
      _lastSavedMobileTx = _todayMobileTx;
    } catch (e) {
      debugPrint('NetFlow: Error cargando uso del día desde BD: $e');
    }

    // Cargar configuración de límite de datos
    _dataLimitEnabled = prefs.getBool('dataLimitEnabled') ?? false;
    if (_dataLimitEnabled) {
      final limitValue = prefs.getDouble('dataLimitValue') ?? 0;
      final limitUnitIndex = prefs.getInt('dataLimitUnit') ?? 2;
      _billingCycleDay = prefs.getInt('billingCycleDay') ?? 1;
      _dataLimitBytes = _convertToBytes(limitValue, limitUnitIndex);

      // Verificar si ya se notificó en este ciclo
      final cycleStart = _getCycleStartDate();
      final cycleKey = '${cycleStart.year}-${cycleStart.month.toString().padLeft(2, '0')}-${cycleStart.day.toString().padLeft(2, '0')}';
      _notifiedCycleKey = prefs.getString('dataLimitNotifiedCycle') ?? '';
      _dataLimitNotified = (_notifiedCycleKey == cycleKey);
    }

    final initialStats = await NetflowTrafficStats.getTrafficStats();
    _lastRxBytes = initialStats['totalRx'] ?? 0;
    _lastTxBytes = initialStats['totalTx'] ?? 0;
    _lastWifiRx = initialStats['wifiRx'] ?? 0;
    _lastWifiTx = initialStats['wifiTx'] ?? 0;
    _lastMobileRx = initialStats['mobileRx'] ?? 0;
    _lastMobileTx = initialStats['mobileTx'] ?? 0;

    final connectivity = await Connectivity().checkConnectivity();
    _lastNetworkType = _getNetworkType(connectivity);

    debugPrint('NetFlow: TaskHandler iniciado en background');
  }

  @override
  void onRepeatEvent(DateTime timestamp) {
    if (_isUpdating) return;

    _isUpdating = true;
    unawaited(
      _updateNetworkStats().whenComplete(() {
        _isUpdating = false;
      }),
    );
  }

  @override
  Future<void> onDestroy(DateTime timestamp, bool isTimeout) async {
    await _saveToDatabase();
    if (_useNativeNotification) {
      await NativeNotificationService.cancel();
      _useNativeNotification = false;
    }
    debugPrint('NetFlow: TaskHandler destruido (timeout: $isTimeout)');
  }

  @override
  void onReceiveData(Object data) {
    if (data is! Map) return;

    final action = data['action'];
    if (action == 'resetTodayStats') {
      _resetTodayStats();
    } else if (action == 'reloadDataLimitConfig') {
      unawaited(_reloadDataLimitConfig());
    }
  }

  Future<void> _reloadDataLimitConfig() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      // Recargar desde disco para obtener cambios del isolate principal
      await prefs.reload();
      _dataLimitEnabled = prefs.getBool('dataLimitEnabled') ?? false;

      if (_dataLimitEnabled) {
        final limitValue = prefs.getDouble('dataLimitValue') ?? 0;
        final limitUnitIndex = prefs.getInt('dataLimitUnit') ?? 2;
        _billingCycleDay = prefs.getInt('billingCycleDay') ?? 1;
        final newLimitBytes = _convertToBytes(limitValue, limitUnitIndex);

        // Si el nuevo límite es mayor, resetear el flag de notificación
        if (newLimitBytes > _dataLimitBytes) {
          _dataLimitNotified = false;
        }

        _dataLimitBytes = newLimitBytes;

        // Verificar si ya se notificó en este ciclo
        final cycleStart = _getCycleStartDate();
        final cycleKey = '${cycleStart.year}-${cycleStart.month.toString().padLeft(2, '0')}-${cycleStart.day.toString().padLeft(2, '0')}';
        _notifiedCycleKey = prefs.getString('dataLimitNotifiedCycle') ?? '';

        // Solo mantener notified si el ciclo coincide Y no se reseteó por límite mayor
        if (_dataLimitNotified) {
          _dataLimitNotified = (_notifiedCycleKey == cycleKey);
        }
      } else {
        _dataLimitBytes = 0;
        _dataLimitNotified = false;
      }

      debugPrint('NetFlow: Config de límite recargada (enabled=$_dataLimitEnabled, limit=$_dataLimitBytes bytes)');

      // Verificar inmediatamente si ya se superó el límite
      if (_dataLimitEnabled && _dataLimitBytes > 0 && !_dataLimitNotified) {
        await _checkDataLimit();
      }
    } catch (e) {
      debugPrint('NetFlow: Error recargando config de límite: $e');
    }
  }

  Future<void> _updateNetworkStats() async {
    final now = DateTime.now();
    final startOfToday = DateTime(now.year, now.month, now.day);

    if (startOfToday.isAfter(_dayStart)) {
      _resetTodayStats(startOfToday: startOfToday);
    }

    final connectivityResult = await Connectivity().checkConnectivity();
    final networkType = _getNetworkType(connectivityResult);
    final networkChanged = networkType != _lastNetworkType;

    if (networkChanged) {
      _downloadSpeedBuffer.clear();
      _uploadSpeedBuffer.clear();
      _lastNetworkType = networkType;
    }

    final stats = await NetflowTrafficStats.getTrafficStats();
    final currentRxBytes = stats['totalRx'] ?? 0;
    final currentTxBytes = stats['totalTx'] ?? 0;
    final currentWifiRx = stats['wifiRx'] ?? 0;
    final currentWifiTx = stats['wifiTx'] ?? 0;
    final currentMobileRx = stats['mobileRx'] ?? 0;
    final currentMobileTx = stats['mobileTx'] ?? 0;

    final elapsedSeconds = now.difference(_lastUpdate).inMilliseconds / 1000.0;
    int downloadSpeed = 0;
    int uploadSpeed = 0;

    if (networkType == NetworkType.none || networkChanged) {
      _downloadSpeedBuffer.clear();
      _uploadSpeedBuffer.clear();

      if (currentRxBytes > 0) {
        _lastRxBytes = currentRxBytes;
        _lastTxBytes = currentTxBytes;
      }
      if (currentWifiRx > 0) {
        _lastWifiRx = currentWifiRx;
        _lastWifiTx = currentWifiTx;
      }
      if (currentMobileRx > 0) {
        _lastMobileRx = currentMobileRx;
        _lastMobileTx = currentMobileTx;
      }
      _lastUpdate = now;
    } else if (elapsedSeconds > 0.1 &&
        _lastRxBytes > 0 &&
        currentRxBytes >= _lastRxBytes) {
      final rawDownloadSpeed =
          ((currentRxBytes - _lastRxBytes) / elapsedSeconds).round();
      final rawUploadSpeed = ((currentTxBytes - _lastTxBytes) / elapsedSeconds)
          .round();

      final safeDownloadSpeed = rawDownloadSpeed < 0 ? 0 : rawDownloadSpeed;
      final safeUploadSpeed = rawUploadSpeed < 0 ? 0 : rawUploadSpeed;

      _downloadSpeedBuffer.add(safeDownloadSpeed);
      _uploadSpeedBuffer.add(safeUploadSpeed);

      if (_downloadSpeedBuffer.length > _speedBufferSize) {
        _downloadSpeedBuffer.removeAt(0);
      }
      if (_uploadSpeedBuffer.length > _speedBufferSize) {
        _uploadSpeedBuffer.removeAt(0);
      }

      downloadSpeed = _downloadSpeedBuffer.isEmpty
          ? 0
          : (_downloadSpeedBuffer.reduce((a, b) => a + b) /
                    _downloadSpeedBuffer.length)
                .round();
      uploadSpeed = _uploadSpeedBuffer.isEmpty
          ? 0
          : (_uploadSpeedBuffer.reduce((a, b) => a + b) /
                    _uploadSpeedBuffer.length)
                .round();

      _todayRxBytes += (currentRxBytes - _lastRxBytes);
      _todayTxBytes += (currentTxBytes - _lastTxBytes);

      if (_lastWifiRx > 0 && currentWifiRx >= _lastWifiRx) {
        _todayWifiRx += (currentWifiRx - _lastWifiRx);
        _todayWifiTx += (currentWifiTx - _lastWifiTx);
      }

      if (_lastMobileRx > 0 && currentMobileRx >= _lastMobileRx) {
        _todayMobileRx += (currentMobileRx - _lastMobileRx);
        _todayMobileTx += (currentMobileTx - _lastMobileTx);
      }
    }

    if (currentRxBytes > 0) {
      _lastRxBytes = currentRxBytes;
      _lastTxBytes = currentTxBytes;
    }
    if (currentWifiRx > 0) {
      _lastWifiRx = currentWifiRx;
      _lastWifiTx = currentWifiTx;
    }
    if (currentMobileRx > 0) {
      _lastMobileRx = currentMobileRx;
      _lastMobileTx = currentMobileTx;
    }

    _lastUpdate = now;

    String? wifiSsid;
    if (networkType == NetworkType.wifi) {
      wifiSsid = await _getWifiSsid();
    } else {
      _cachedWifiSsid = null;
      _lastSsidUpdate = null;
    }

    final speedText = DataFormatter.formatSpeedWithLabels(
      download: downloadSpeed,
      upload: uploadSpeed,
      useBits: _useBitsUnit,
    );

    String connectionInfo;
    if (networkType == NetworkType.wifi &&
        wifiSsid != null &&
        wifiSsid.isNotEmpty) {
      connectionInfo = 'WiFi: $wifiSsid';
    } else {
      connectionInfo = networkType.displayName;
    }

    final bodyText =
        '$connectionInfo | '
        'WiFi: ${DataFormatter.formatBytes(_todayWifiRx + _todayWifiTx)} · '
        'Móvil: ${DataFormatter.formatBytes(_todayMobileRx + _todayMobileTx)}';

    if (_useNativeNotification) {
      final updated = await NativeNotificationService.updateNotification(
        downloadSpeed: downloadSpeed,
        uploadSpeed: uploadSpeed,
        title: speedText,
        text: bodyText,
      );

      if (!updated) {
        // Fallback al canal del plugin si el canal nativo falla temporalmente.
        FlutterForegroundTask.updateService(
          notificationTitle: speedText,
          notificationText: bodyText,
        );
      }
    } else {
      FlutterForegroundTask.updateService(
        notificationTitle: speedText,
        notificationText: bodyText,
      );
    }

    FlutterForegroundTask.sendDataToMain({
      'downloadSpeed': downloadSpeed,
      'uploadSpeed': uploadSpeed,
      'networkType': networkType.index,
      'todayDownload': _todayRxBytes,
      'todayUpload': _todayTxBytes,
      'todayWifiTotal': _todayWifiRx + _todayWifiTx,
      'todayMobileTotal': _todayMobileRx + _todayMobileTx,
    });

    _dbTickCounter += 1;
    if (_dbTickCounter >= _dbSaveIntervalTicks) {
      _dbTickCounter = 0;
      await _saveToDatabase();
      await _checkDataLimit();
    }
  }

  Future<void> _saveToDatabase() async {
    final wifiRxDelta = _todayWifiRx - _lastSavedWifiRx;
    final wifiTxDelta = _todayWifiTx - _lastSavedWifiTx;
    final mobileRxDelta = _todayMobileRx - _lastSavedMobileRx;
    final mobileTxDelta = _todayMobileTx - _lastSavedMobileTx;

    if (wifiRxDelta > 1024 || wifiTxDelta > 1024) {
      await DatabaseService.updateTodayUsage(
        receivedDelta: wifiRxDelta,
        sentDelta: wifiTxDelta,
        networkType: NetworkType.wifi,
      );
      _lastSavedWifiRx = _todayWifiRx;
      _lastSavedWifiTx = _todayWifiTx;
    }

    if (mobileRxDelta > 1024 || mobileTxDelta > 1024) {
      await DatabaseService.updateTodayUsage(
        receivedDelta: mobileRxDelta,
        sentDelta: mobileTxDelta,
        networkType: NetworkType.mobile,
      );
      _lastSavedMobileRx = _todayMobileRx;
      _lastSavedMobileTx = _todayMobileTx;
    }
  }

  Future<String?> _getWifiSsid() async {
    try {
      final hasLocationPermission =
          await Permission.locationWhenInUse.isGranted ||
          await Permission.location.isGranted;
      if (!hasLocationPermission) {
        return _cachedWifiSsid;
      }

      final locationServiceStatus =
          await Permission.locationWhenInUse.serviceStatus;
      if (locationServiceStatus != ServiceStatus.enabled) {
        debugPrint('NetFlow: Ubicación del sistema desactivada, usando SSID en cache');
        return _cachedWifiSsid;
      }

      if (_cachedWifiSsid != null && _lastSsidUpdate != null) {
        final elapsed = DateTime.now().difference(_lastSsidUpdate!);
        if (elapsed < _ssidCacheDuration) {
          return _cachedWifiSsid;
        }
      }

      final networkInfo = NetworkInfo();
      final ssid = _sanitizeSsid(await networkInfo.getWifiName());

      if (ssid != null) {
        _cachedWifiSsid = ssid;
        _lastSsidUpdate = DateTime.now();
        return ssid;
      }

      // Algunos dispositivos devuelven temporalmente "unknown ssid" en background.
      // Conservamos el último valor válido mientras siga en WiFi.
      if (_cachedWifiSsid != null) {
        return _cachedWifiSsid;
      }

      _lastSsidUpdate = DateTime.now();
      return null;
    } catch (e) {
      debugPrint('NetFlow: Error obteniendo SSID en background: $e');
      return _cachedWifiSsid;
    }
  }

  String? _sanitizeSsid(String? ssid) {
    if (ssid == null) return null;

    final cleaned = ssid.replaceAll('"', '').trim();
    if (cleaned.isEmpty || cleaned.toLowerCase() == '<unknown ssid>') {
      return null;
    }

    return cleaned;
  }

  NetworkType _getNetworkType(List<ConnectivityResult> results) {
    if (results.contains(ConnectivityResult.wifi)) {
      return NetworkType.wifi;
    }
    if (results.contains(ConnectivityResult.mobile)) {
      return NetworkType.mobile;
    }
    return NetworkType.none;
  }

  void _resetTodayStats({DateTime? startOfToday}) {
    _todayRxBytes = 0;
    _todayTxBytes = 0;
    _todayWifiRx = 0;
    _todayWifiTx = 0;
    _todayMobileRx = 0;
    _todayMobileTx = 0;

    _lastSavedWifiRx = 0;
    _lastSavedWifiTx = 0;
    _lastSavedMobileRx = 0;
    _lastSavedMobileTx = 0;

    _downloadSpeedBuffer.clear();
    _uploadSpeedBuffer.clear();

    _dayStart =
        startOfToday ??
        DateTime(DateTime.now().year, DateTime.now().month, DateTime.now().day);
    _dbTickCounter = 0;

    FlutterForegroundTask.sendDataToMain({
      'downloadSpeed': 0,
      'uploadSpeed': 0,
      'networkType': _lastNetworkType.index,
      'todayDownload': 0,
      'todayUpload': 0,
      'todayWifiTotal': 0,
      'todayMobileTotal': 0,
    });
  }

  Future<void> _checkDataLimit() async {
    if (!_dataLimitEnabled || _dataLimitBytes <= 0 || _dataLimitNotified) return;

    try {
      final cycleStart = _getCycleStartDate();
      final now = DateTime.now();
      final today = DateTime(now.year, now.month, now.day);

      // Obtener uso acumulado del ciclo desde la BD
      final cycleUsage = await DatabaseService.getTotalUsageInRange(cycleStart, today);
      final dbTotal = (cycleUsage['wifiReceived'] ?? 0) +
          (cycleUsage['wifiSent'] ?? 0) +
          (cycleUsage['mobileReceived'] ?? 0) +
          (cycleUsage['mobileSent'] ?? 0);

      // Sumar deltas en memoria aún no guardados en BD
      final unsavedWifiRx = _todayWifiRx - _lastSavedWifiRx;
      final unsavedWifiTx = _todayWifiTx - _lastSavedWifiTx;
      final unsavedMobileRx = _todayMobileRx - _lastSavedMobileRx;
      final unsavedMobileTx = _todayMobileTx - _lastSavedMobileTx;
      final unsavedTotal = unsavedWifiRx + unsavedWifiTx + unsavedMobileRx + unsavedMobileTx;

      final totalUsage = dbTotal + unsavedTotal;

      if (totalUsage >= _dataLimitBytes) {
        final usageFormatted = DataFormatter.formatBytes(totalUsage);
        final limitFormatted = DataFormatter.formatBytes(_dataLimitBytes);

        await NativeNotificationService.showDataLimitAlert(
          title: 'Límite de datos alcanzado',
          text: 'Has usado $usageFormatted de tu límite de $limitFormatted',
        );

        _dataLimitNotified = true;

        // Guardar flag para que persista entre reinicios del servicio
        final cycleKey = '${cycleStart.year}-${cycleStart.month.toString().padLeft(2, '0')}-${cycleStart.day.toString().padLeft(2, '0')}';
        final prefs = await SharedPreferences.getInstance();
        await prefs.setString('dataLimitNotifiedCycle', cycleKey);

        debugPrint('NetFlow: Límite de datos alcanzado ($usageFormatted / $limitFormatted)');
      }
    } catch (e) {
      debugPrint('NetFlow: Error verificando límite de datos: $e');
    }
  }

  DateTime _getCycleStartDate() {
    final now = DateTime.now();
    if (now.day >= _billingCycleDay) {
      return DateTime(now.year, now.month, _billingCycleDay);
    } else {
      // Mes anterior
      final prevMonth = now.month == 1
          ? DateTime(now.year - 1, 12, _billingCycleDay)
          : DateTime(now.year, now.month - 1, _billingCycleDay);
      return prevMonth;
    }
  }

  int _convertToBytes(double value, int unitIndex) {
    // 0 = KB, 1 = MB, 2 = GB
    switch (unitIndex) {
      case 0:
        return (value * 1024).round();
      case 1:
        return (value * 1024 * 1024).round();
      case 2:
        return (value * 1024 * 1024 * 1024).round();
      default:
        return (value * 1024 * 1024 * 1024).round();
    }
  }
}

/// Servicio principal para gestionar el monitoreo de red.
/// Arquitectura robusta: el polling vive en TaskHandler (background isolate).
class NetworkMonitorService {
  NetworkMonitorService._();

  static const int _foregroundServiceId = 1000;

  static StreamController<NetworkStats>? _statsController;
  static bool _isTaskDataCallbackRegistered = false;

  static NetworkStats _currentStats = const NetworkStats(
    downloadSpeed: 0,
    uploadSpeed: 0,
    networkType: NetworkType.none,
    todayDownload: 0,
    todayUpload: 0,
  );

  /// Stream de estadísticas de red en tiempo real.
  static Stream<NetworkStats> get statsStream {
    _statsController ??= StreamController<NetworkStats>.broadcast();
    return _statsController!.stream;
  }

  static void _registerTaskDataCallback() {
    if (_isTaskDataCallbackRegistered) return;

    FlutterForegroundTask.addTaskDataCallback(_onReceiveTaskData);
    _isTaskDataCallbackRegistered = true;
  }

  static void _unregisterTaskDataCallback() {
    if (!_isTaskDataCallbackRegistered) return;

    FlutterForegroundTask.removeTaskDataCallback(_onReceiveTaskData);
    _isTaskDataCallbackRegistered = false;
  }

  static void _onReceiveTaskData(Object data) {
    if (data is! Map) return;

    final downloadSpeed = (data['downloadSpeed'] as num?)?.toInt() ?? 0;
    final uploadSpeed = (data['uploadSpeed'] as num?)?.toInt() ?? 0;
    final todayDownload = (data['todayDownload'] as num?)?.toInt() ?? 0;
    final todayUpload = (data['todayUpload'] as num?)?.toInt() ?? 0;
    final networkTypeIndex = (data['networkType'] as num?)?.toInt() ?? 2;

    final networkType =
        (networkTypeIndex >= 0 && networkTypeIndex < NetworkType.values.length)
        ? NetworkType.values[networkTypeIndex]
        : NetworkType.none;

    _currentStats = NetworkStats(
      downloadSpeed: downloadSpeed,
      uploadSpeed: uploadSpeed,
      networkType: networkType,
      todayDownload: todayDownload,
      todayUpload: todayUpload,
    );

    _statsController?.add(_currentStats);
  }

  /// Inicializa el servicio de foreground.
  static Future<void> init() async {
    FlutterForegroundTask.initCommunicationPort();

    final prefs = await SharedPreferences.getInstance();
    final hideOnLockscreen = prefs.getBool('hideOnLockscreen') ?? false;
    final visibility = hideOnLockscreen
        ? NotificationVisibility.VISIBILITY_SECRET
        : NotificationVisibility.VISIBILITY_PUBLIC;

    final channelId = hideOnLockscreen
        ? 'netflow_monitor_private'
        : 'netflow_monitor';

    FlutterForegroundTask.init(
      androidNotificationOptions: AndroidNotificationOptions(
        channelId: channelId,
        channelName: 'NetFlow Monitor',
        channelDescription: 'Monitoreo de uso de datos en tiempo real',
        channelImportance: NotificationChannelImportance.LOW,
        priority: NotificationPriority.LOW,
        showWhen: false,
        enableVibration: false,
        playSound: false,
        visibility: visibility,
      ),
      iosNotificationOptions: const IOSNotificationOptions(
        showNotification: false,
        playSound: false,
      ),
      foregroundTaskOptions: ForegroundTaskOptions(
        eventAction: ForegroundTaskEventAction.repeat(1000),
        autoRunOnBoot: true,
        autoRunOnMyPackageReplaced: true,
        allowWakeLock: true,
        allowWifiLock: true,
        allowAutoRestart: true,
        stopWithTask: false,
      ),
    );
  }

  /// Inicia el servicio de monitoreo.
  static Future<bool> start() async {
    _registerTaskDataCallback();

    if (await FlutterForegroundTask.isRunningService) {
      return true;
    }

    final result = await FlutterForegroundTask.startService(
      serviceId: _foregroundServiceId,
      notificationTitle: 'NetFlow',
      notificationText: 'Iniciando monitoreo...',
      callback: startCallback,
    );

    return switch (result) {
      ServiceRequestSuccess() => true,
      ServiceRequestFailure() => false,
    };
  }

  /// Detiene el servicio de monitoreo.
  static Future<bool> stop() async {
    _unregisterTaskDataCallback();

    final result = await FlutterForegroundTask.stopService();
    final success = switch (result) {
      ServiceRequestSuccess() => true,
      ServiceRequestFailure() => false,
    };

    if (success) {
      await NativeNotificationService.cancel();
      _currentStats = const NetworkStats(
        downloadSpeed: 0,
        uploadSpeed: 0,
        networkType: NetworkType.none,
        todayDownload: 0,
        todayUpload: 0,
      );
      _statsController?.add(_currentStats);
    }

    return success;
  }

  /// Reinicia el servicio (útil cuando cambian configuraciones de notificación).
  static Future<bool> restart() async {
    final wasRunning = await isRunning;
    if (wasRunning) {
      await stop();
      await Future.delayed(const Duration(milliseconds: 300));
    }

    await init();
    return start();
  }

  /// Verifica si el servicio está corriendo.
  static Future<bool> get isRunning async {
    return FlutterForegroundTask.isRunningService;
  }

  /// Reinicia los contadores del día desde el TaskHandler.
  static void resetTodayStats() {
    FlutterForegroundTask.sendDataToTask({'action': 'resetTodayStats'});

    _currentStats = _currentStats.copyWith(
      downloadSpeed: 0,
      uploadSpeed: 0,
      todayDownload: 0,
      todayUpload: 0,
    );
    _statsController?.add(_currentStats);
  }

  /// Recarga la configuración de límite de datos en el TaskHandler.
  static void reloadDataLimitConfig() {
    FlutterForegroundTask.sendDataToTask({'action': 'reloadDataLimitConfig'});
  }

  /// Libera recursos del servicio.
  static void dispose() {
    _unregisterTaskDataCallback();
    _statsController?.close();
    _statsController = null;
  }
}

/// Modelo para estadísticas de red en tiempo real.
class NetworkStats {
  final int downloadSpeed;
  final int uploadSpeed;
  final NetworkType networkType;
  final int todayDownload;
  final int todayUpload;

  const NetworkStats({
    required this.downloadSpeed,
    required this.uploadSpeed,
    required this.networkType,
    required this.todayDownload,
    required this.todayUpload,
  });

  int get todayTotal => todayDownload + todayUpload;

  NetworkStats copyWith({
    int? downloadSpeed,
    int? uploadSpeed,
    NetworkType? networkType,
    int? todayDownload,
    int? todayUpload,
  }) {
    return NetworkStats(
      downloadSpeed: downloadSpeed ?? this.downloadSpeed,
      uploadSpeed: uploadSpeed ?? this.uploadSpeed,
      networkType: networkType ?? this.networkType,
      todayDownload: todayDownload ?? this.todayDownload,
      todayUpload: todayUpload ?? this.todayUpload,
    );
  }
}

/// Widget wrapper para compatibilidad.
class WithForegroundTask extends StatelessWidget {
  final Widget child;

  const WithForegroundTask({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    return child;
  }
}
