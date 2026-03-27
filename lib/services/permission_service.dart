import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';

import '../theme/app_theme.dart';

/// Servicio para gestionar permisos de la aplicación
class PermissionService {
  PermissionService._();

  /// Lista de permisos necesarios para el monitoreo de red
  static const List<Permission> _requiredPermissions = [
    Permission.phone, // READ_PHONE_STATE para datos móviles
    Permission.notification, // POST_NOTIFICATIONS para Android 13+
  ];

  /// Verifica si todos los permisos necesarios están otorgados
  static Future<bool> hasAllPermissions() async {
    for (final permission in _requiredPermissions) {
      if (!await permission.isGranted) {
        return false;
      }
    }
    return true;
  }

  /// Solicita todos los permisos necesarios
  /// Retorna true si todos fueron otorgados
  static Future<bool> requestAllPermissions() async {
    final statuses = await _requiredPermissions.request();
    
    return statuses.values.every(
      (status) => status.isGranted || status.isLimited,
    );
  }

  /// Verifica el estado de un permiso específico
  static Future<PermissionStatus> checkPermission(Permission permission) async {
    return await permission.status;
  }

  /// Solicita un permiso específico
  static Future<PermissionStatus> requestPermission(Permission permission) async {
    return await permission.request();
  }

  /// Verifica si el permiso de notificaciones está otorgado
  static Future<bool> hasNotificationPermission() async {
    return await Permission.notification.isGranted;
  }

  /// Solicita permiso de notificaciones
  static Future<bool> requestNotificationPermission() async {
    final status = await Permission.notification.request();
    return status.isGranted;
  }

  /// Verifica si el permiso de teléfono está otorgado
  static Future<bool> hasPhonePermission() async {
    return await Permission.phone.isGranted;
  }

  /// Solicita permiso de teléfono
  static Future<bool> requestPhonePermission() async {
    final status = await Permission.phone.request();
    return status.isGranted;
  }

  /// Abre la configuración de la app si el permiso fue denegado permanentemente
  static Future<bool> openSettings() async {
    return await openAppSettings();
  }

  /// Muestra un diálogo explicando por qué se necesitan los permisos
  static Future<bool> showPermissionRationale(
    BuildContext context, {
    required String title,
    required String message,
    String confirmText = 'Continuar',
    String cancelText = 'Cancelar',
  }) async {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    final result = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        title: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: colorScheme.primaryContainer,
                shape: BoxShape.circle,
              ),
              child: Icon(
                Icons.security_rounded,
                color: colorScheme.primary,
                size: 24,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                title,
                style: theme.textTheme.titleLarge?.copyWith(
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            _PermissionItem(
              icon: Icons.phone_android_rounded,
              title: 'Teléfono',
              description: 'Para monitorear el uso de datos móviles',
              theme: theme,
              colorScheme: colorScheme,
            ),
            const SizedBox(height: 12),
            _PermissionItem(
              icon: Icons.notifications_rounded,
              title: 'Notificaciones',
              description: 'Para mostrar el uso de datos en tiempo real',
              theme: theme,
              colorScheme: colorScheme,
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: Text(cancelText),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: Text(confirmText),
          ),
        ],
      ),
    );
    return result ?? false;
  }

  /// Flujo completo de solicitud de permisos con UI
  static Future<bool> requestPermissionsWithUI(BuildContext context) async {
    // Verificar si ya tiene todos los permisos
    if (await hasAllPermissions()) {
      return true;
    }

    // Verificar que el context siga montado
    if (!context.mounted) return false;

    // Mostrar explicación al usuario
    final shouldContinue = await showPermissionRationale(
      context,
      title: 'Permisos necesarios',
      message: 'NetFlow necesita los siguientes permisos para funcionar:\n\n'
          '• Teléfono: Para monitorear el uso de datos móviles\n'
          '• Notificaciones: Para mostrar el uso de datos en tiempo real',
    );

    if (!shouldContinue) {
      return false;
    }

    // Solicitar permisos
    final granted = await requestAllPermissions();

    if (!granted && context.mounted) {
      // Verificar si algún permiso fue denegado permanentemente
      bool permanentlyDenied = false;
      for (final permission in _requiredPermissions) {
        if (await permission.isPermanentlyDenied) {
          permanentlyDenied = true;
          break;
        }
      }

      if (permanentlyDenied && context.mounted) {
        final openSettingsResult = await showPermissionRationale(
          context,
          title: 'Permisos requeridos',
          message: 'Algunos permisos fueron denegados. '
              'Por favor, habilítalos manualmente en la configuración de la aplicación.',
          confirmText: 'Abrir configuración',
          cancelText: 'Cancelar',
        );

        if (openSettingsResult) {
          await openSettings();
        }
      }
    }

    return granted;
  }

  /// Solicita permiso de ubicación (necesario para obtener SSID de WiFi)
  static Future<bool> requestLocationPermission() async {
    final status = await Permission.locationWhenInUse.request();
    if (!status.isGranted && !status.isLimited) {
      return false;
    }

    // Intentar habilitar acceso en segundo plano para mantener SSID en background.
    final alwaysStatus = await Permission.locationAlways.status;
    if (!alwaysStatus.isGranted) {
      await Permission.locationAlways.request();
    }

    return true;
  }

  /// Verifica si el permiso de ubicación está otorgado
  static Future<bool> hasLocationPermission() async {
    final whenInUseGranted = await Permission.locationWhenInUse.isGranted;
    if (whenInUseGranted) return true;

    return await Permission.location.isGranted;
  }

  /// Verifica si la app tiene permiso de ubicación en segundo plano
  static Future<bool> hasBackgroundLocationPermission() async {
    return await Permission.locationAlways.isGranted;
  }

  /// Verifica si la ubicación del sistema está activada (GPS/ubicación)
  static Future<bool> hasLocationServiceEnabled() async {
    final status = await Permission.locationWhenInUse.serviceStatus;
    return status == ServiceStatus.enabled;
  }

  /// Solicita exención de optimización de batería
  /// Esto permite que el servicio funcione en segundo plano sin restricciones
  static Future<bool> requestBatteryOptimizationExemption() async {
    final status = await Permission.ignoreBatteryOptimizations.request();
    return status.isGranted;
  }

  /// Verifica si la app está exenta de optimización de batería
  static Future<bool> hasBatteryOptimizationExemption() async {
    return await Permission.ignoreBatteryOptimizations.isGranted;
  }
}

/// Widget para mostrar un item de permiso en el diálogo
class _PermissionItem extends StatelessWidget {
  final IconData icon;
  final String title;
  final String description;
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _PermissionItem({
    required this.icon,
    required this.title,
    required this.description,
    required this.theme,
    required this.colorScheme,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: colorScheme.primaryContainer,
              shape: BoxShape.circle,
            ),
            child: Icon(
              icon,
              color: colorScheme.primary,
              size: 20,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: theme.textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
                ),
                Text(
                  description,
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: colorScheme.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
