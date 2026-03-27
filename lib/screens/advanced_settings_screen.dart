import 'package:flutter/material.dart';

import '../services/permission_service.dart';
import '../theme/app_theme.dart';

/// Pantalla de opciones avanzadas
class AdvancedSettingsScreen extends StatefulWidget {
  const AdvancedSettingsScreen({super.key});

  @override
  State<AdvancedSettingsScreen> createState() => _AdvancedSettingsScreenState();
}

class _AdvancedSettingsScreenState extends State<AdvancedSettingsScreen> {
  bool _hasLocationPermission = false;
  bool _hasBackgroundLocationPermission = false;
  bool _hasLocationServiceEnabled = false;
  bool _hasBatteryExemption = false;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _checkPermissions();
  }

  Future<void> _checkPermissions() async {
    final locationStatus = await PermissionService.hasLocationPermission();
    final backgroundLocationStatus =
        await PermissionService.hasBackgroundLocationPermission();
    final locationServiceStatus =
        await PermissionService.hasLocationServiceEnabled();
    final batteryStatus = await PermissionService.hasBatteryOptimizationExemption();
    
    if (mounted) {
      setState(() {
        _hasLocationPermission = locationStatus;
        _hasBackgroundLocationPermission = backgroundLocationStatus;
        _hasLocationServiceEnabled = locationServiceStatus;
        _hasBatteryExemption = batteryStatus;
        _isLoading = false;
      });
    }
  }

  Future<void> _requestLocationPermission() async {
    final granted = await PermissionService.requestLocationPermission();
    await _checkPermissions();

    if (mounted) {
      if (granted) {
        if (!_hasBackgroundLocationPermission) {
          _showSnackBar(
            'Ubicacion concedida. Para SSID estable en segundo plano, habilita "Permitir todo el tiempo" en Ajustes.',
          );
        } else if (!_hasLocationServiceEnabled) {
          _showSnackBar(
            'Permiso concedido. Activa la ubicacion del sistema para que se muestre el SSID.',
          );
        } else {
          _showSnackBar('Permisos de ubicacion actualizados correctamente');
        }
      } else {
        _showSnackBar('Permiso denegado. Puedes habilitarlo en ajustes del sistema.');
      }
    }
  }

  Future<void> _requestBatteryExemption() async {
    final granted = await PermissionService.requestBatteryOptimizationExemption();
    // Re-check after returning from system settings
    await Future.delayed(const Duration(milliseconds: 500));
    await _checkPermissions();
    
    if (mounted && granted) {
      _showSnackBar('Optimización de batería desactivada');
    }
  }

  void _showSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Opciones avanzadas'),
        centerTitle: true,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                // Información
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: colorScheme.primaryContainer.withValues(alpha: 0.3),
                    borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
                  ),
                  child: Row(
                    children: [
                      Icon(
                        Icons.info_outline_rounded,
                        color: colorScheme.primary,
                        size: 24,
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          'Estos permisos mejoran la funcionalidad de la app pero no son obligatorios.',
                          style: theme.textTheme.bodyMedium?.copyWith(
                            color: colorScheme.onSurface,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 24),

                // Opción: WiFi SSID
                _AdvancedOptionTile(
                  icon: Icons.wifi_rounded,
                  title: 'Mostrar nombre de WiFi',
                  description:
                    'Permite ver el nombre (SSID) de la red WiFi conectada en la notificacion. Para segundo plano, se recomienda permitir ubicacion todo el tiempo y mantener la ubicacion del sistema activa.',
                  permissionInfo:
                    'Requiere ubicacion + acceso en segundo plano',
                  isGranted: _hasLocationPermission &&
                    _hasBackgroundLocationPermission &&
                    _hasLocationServiceEnabled,
                  onTap: (_hasLocationPermission &&
                      _hasBackgroundLocationPermission &&
                      _hasLocationServiceEnabled)
                    ? null
                    : _requestLocationPermission,
                  theme: theme,
                  colorScheme: colorScheme,
                ),
                const SizedBox(height: 16),

                // Opción: Optimización de batería
                _AdvancedOptionTile(
                  icon: Icons.battery_saver_rounded,
                  title: 'Excluir de ahorro de batería',
                  description:
                      'Evita que Android detenga el servicio de monitoreo cuando la app está en segundo plano.',
                  permissionInfo: 'Requiere excepción de optimización',
                  isGranted: _hasBatteryExemption,
                  onTap: _hasBatteryExemption ? null : _requestBatteryExemption,
                  theme: theme,
                  colorScheme: colorScheme,
                ),
              ],
            ),
    );
  }
}

/// Widget para mostrar una opción avanzada con estado de permiso
class _AdvancedOptionTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String description;
  final String permissionInfo;
  final bool isGranted;
  final VoidCallback? onTap;
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _AdvancedOptionTile({
    required this.icon,
    required this.title,
    required this.description,
    required this.permissionInfo,
    required this.isGranted,
    required this.onTap,
    required this.theme,
    required this.colorScheme,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
        border: isGranted
            ? Border.all(color: colorScheme.primary.withValues(alpha: 0.3), width: 1.5)
            : null,
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: isGranted
                            ? colorScheme.primaryContainer
                            : colorScheme.surfaceContainerHighest,
                        shape: BoxShape.circle,
                      ),
                      child: Icon(
                        icon,
                        color: isGranted ? colorScheme.primary : colorScheme.onSurfaceVariant,
                        size: 24,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            title,
                            style: theme.textTheme.titleMedium?.copyWith(
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                          const SizedBox(height: 2),
                          Row(
                            children: [
                              Icon(
                                isGranted
                                    ? Icons.check_circle_rounded
                                    : Icons.radio_button_unchecked_rounded,
                                size: 14,
                                color: isGranted
                                    ? colorScheme.primary
                                    : colorScheme.onSurfaceVariant,
                              ),
                              const SizedBox(width: 4),
                              Text(
                                isGranted ? 'Activado' : 'No activado',
                                style: theme.textTheme.bodySmall?.copyWith(
                                  color: isGranted
                                      ? colorScheme.primary
                                      : colorScheme.onSurfaceVariant,
                                  fontWeight: isGranted ? FontWeight.w600 : FontWeight.normal,
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                    if (!isGranted)
                      FilledButton.tonal(
                        onPressed: onTap,
                        child: const Text('Activar'),
                      ),
                  ],
                ),
                const SizedBox(height: 12),
                Text(
                  description,
                  style: theme.textTheme.bodyMedium?.copyWith(
                    color: colorScheme.onSurfaceVariant,
                  ),
                ),
                const SizedBox(height: 8),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  decoration: BoxDecoration(
                    color: colorScheme.surfaceContainerHighest,
                    borderRadius: BorderRadius.circular(AppTheme.radiusSmall),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        Icons.security_rounded,
                        size: 14,
                        color: colorScheme.onSurfaceVariant,
                      ),
                      const SizedBox(width: 6),
                      Text(
                        permissionInfo,
                        style: theme.textTheme.labelSmall?.copyWith(
                          color: colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
