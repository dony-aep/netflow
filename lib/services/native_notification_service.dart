import 'package:flutter/services.dart';

/// Servicio para manejar la notificación nativa con icono de velocidad dinámico
/// Solo disponible en Android
class NativeNotificationService {
  NativeNotificationService._();
  
  static const MethodChannel _channel = MethodChannel('com.netflow.app/notification');
  
  static bool _isInitialized = false;
  
  /// Inicializa el canal de notificación
  static Future<bool> init({bool hideOnLockscreen = false}) async {
    try {
      final result = await _channel.invokeMethod<bool>('initChannel', {
        'hideOnLockscreen': hideOnLockscreen,
      });
      _isInitialized = result ?? false;
      return _isInitialized;
    } catch (e) {
      _isInitialized = false;
      return false;
    }
  }
  
  /// Actualiza la notificación con la velocidad actual
  /// El icono mostrará la velocidad mayor (bajada o subida)
  static Future<bool> updateNotification({
    required int downloadSpeed,
    required int uploadSpeed,
    required String title,
    required String text,
  }) async {
    if (!_isInitialized) return false;
    
    try {
      final result = await _channel.invokeMethod<bool>('updateNotification', {
        'downloadSpeed': downloadSpeed,
        'uploadSpeed': uploadSpeed,
        'title': title,
        'text': text,
      });
      return result ?? false;
    } catch (e) {
      return false;
    }
  }
  
  /// Cancela la notificación
  static Future<bool> cancel() async {
    try {
      final result = await _channel.invokeMethod<bool>('cancelNotification');
      _isInitialized = false;
      return result ?? false;
    } catch (e) {
      return false;
    }
  }

  /// Muestra una notificación de alerta cuando se alcanza el límite de datos
  static Future<bool> showDataLimitAlert({
    required String title,
    required String text,
  }) async {
    try {
      final result = await _channel.invokeMethod<bool>('showDataLimitAlert', {
        'title': title,
        'text': text,
      });
      return result ?? false;
    } catch (e) {
      return false;
    }
  }
  
  /// Indica si el servicio está inicializado
  static bool get isInitialized => _isInitialized;
}
