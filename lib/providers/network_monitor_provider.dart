import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:connectivity_plus/connectivity_plus.dart';

import '../models/models.dart';
import '../services/services.dart';

/// Provider para gestionar el estado del monitoreo de red
/// El monitoreo se inicia automáticamente al abrir la app
class NetworkMonitorProvider extends ChangeNotifier {
  // Estado del servicio
  bool _isMonitoring = false;

  // Estadísticas actuales
  NetworkStats _currentStats = const NetworkStats(
    downloadSpeed: 0,
    uploadSpeed: 0,
    networkType: NetworkType.none,
    todayDownload: 0,
    todayUpload: 0,
  );

  // Uso del día de la base de datos
  DailyUsage? _todayUsage;

  // Suscripciones
  StreamSubscription<NetworkStats>? _statsSubscription;
  StreamSubscription<List<ConnectivityResult>>? _connectivitySubscription;

  // Getters
  bool get isMonitoring => _isMonitoring;
  NetworkStats get currentStats => _currentStats;
  DailyUsage? get todayUsage => _todayUsage;
  NetworkType get currentNetworkType => _currentStats.networkType;
  int get downloadSpeed => _currentStats.downloadSpeed;
  int get uploadSpeed => _currentStats.uploadSpeed;
  int get todayDownload => _currentStats.todayDownload;
  int get todayUpload => _currentStats.todayUpload;
  int get todayTotal => _currentStats.todayDownload + _currentStats.todayUpload;

  /// Inicializa el provider e inicia el monitoreo automáticamente
  Future<void> init() async {
    // Escuchar cambios de conectividad
    _setupConnectivityListener();
    
    // Cargar uso de hoy desde la base de datos
    await _loadTodayUsage();
    
    // Iniciar monitoreo automáticamente
    await startMonitoring();
    
    notifyListeners();
  }

  /// Inicia el monitoreo de red (se llama automáticamente al iniciar la app)
  Future<bool> startMonitoring() async {
    if (_isMonitoring) return true;

    try {
      await NetworkMonitorService.init();
      final started = await NetworkMonitorService.start();

      if (started) {
        _isMonitoring = true;
        _subscribeToStats();
        notifyListeners();
      }

      return started;
    } catch (e) {
      debugPrint('NetFlow: Error iniciando monitoreo: $e');
      return false;
    }
  }

  /// Detiene el monitoreo de red (solo se llama al cerrar la app)
  Future<bool> stopMonitoring() async {
    if (!_isMonitoring) return true;

    try {
      final stopped = await NetworkMonitorService.stop();

      if (stopped) {
        _isMonitoring = false;
        _statsSubscription?.cancel();
        _statsSubscription = null;
        
        // Resetear estadísticas de velocidad
        _currentStats = _currentStats.copyWith(
          downloadSpeed: 0,
          uploadSpeed: 0,
        );
        notifyListeners();
      }

      return stopped;
    } catch (e) {
      debugPrint('NetFlow: Error deteniendo monitoreo: $e');
      return false;
    }
  }

  /// Suscribe al stream de estadísticas
  void _subscribeToStats() {
    _statsSubscription?.cancel();
    _statsSubscription = NetworkMonitorService.statsStream.listen((stats) {
      _currentStats = stats;
      notifyListeners();
    });
  }

  /// Configura el listener de cambios de conectividad
  void _setupConnectivityListener() {
    _connectivitySubscription?.cancel();
    _connectivitySubscription = Connectivity().onConnectivityChanged.listen(
      (List<ConnectivityResult> results) {
        // Actualizar tipo de red si no está monitoreando activamente
        if (!_isMonitoring) {
          NetworkType newType;
          if (results.contains(ConnectivityResult.wifi)) {
            newType = NetworkType.wifi;
          } else if (results.contains(ConnectivityResult.mobile)) {
            newType = NetworkType.mobile;
          } else {
            newType = NetworkType.none;
          }
          
          _currentStats = _currentStats.copyWith(networkType: newType);
          notifyListeners();
        }
      },
    );
  }

  /// Carga el uso de hoy desde la base de datos
  Future<void> _loadTodayUsage() async {
    try {
      _todayUsage = await DatabaseService.getTodayUsage();
      notifyListeners();
    } catch (e) {
      // Ignorar errores de carga inicial
    }
  }

  /// Recarga el uso de hoy
  Future<void> refreshTodayUsage() async {
    await _loadTodayUsage();
  }

  /// Reinicia los contadores del día
  void resetTodayStats() {
    NetworkMonitorService.resetTodayStats();
    _currentStats = _currentStats.copyWith(
      todayDownload: 0,
      todayUpload: 0,
    );
    notifyListeners();
  }

  @override
  void dispose() {
    _statsSubscription?.cancel();
    _connectivitySubscription?.cancel();
    super.dispose();
  }
}
