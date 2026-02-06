import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/models.dart';
import '../services/services.dart';
import '../providers/providers.dart';
import '../theme/app_theme.dart';
import 'history_screen.dart';
import 'settings_screen.dart';

/// Pantalla principal de NetFlow
/// Muestra el uso de datos en tiempo real y controles del servicio
/// Diseño: Material Design 3 Expressive
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  @override
  void initState() {
    super.initState();
    // Verificar permisos al iniciar
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _checkPermissions();
    });
  }

  Future<void> _checkPermissions() async {
    // Verificar si ya tiene todos los permisos
    final hasPermissions = await PermissionService.hasAllPermissions();
    
    if (!hasPermissions && mounted) {
      await _showPermissionsDialog();
    }

    if (!mounted) return;
    await _checkBatteryOptimizationExemption();
  }

  Future<void> _checkBatteryOptimizationExemption() async {
    final hasBatteryExemption =
        await PermissionService.hasBatteryOptimizationExemption();
    if (hasBatteryExemption || !mounted) return;

    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    final shouldRequest = await showDialog<bool>(
      context: context,
      barrierDismissible: true,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppTheme.radiusLarge),
        ),
        contentPadding: const EdgeInsets.fromLTRB(24, 20, 24, 24),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Icono
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: colorScheme.tertiaryContainer,
                shape: BoxShape.circle,
              ),
              child: Icon(
                Icons.battery_saver_rounded,
                size: 40,
                color: colorScheme.onTertiaryContainer,
              ),
            ),
            const SizedBox(height: 20),
            // Título
            Text(
              'Optimización de batería',
              style: theme.textTheme.headlineSmall?.copyWith(
                fontWeight: FontWeight.w600,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 12),
            // Descripción
            Text(
              'Para que NetFlow funcione correctamente en segundo plano, necesita estar excluida del ahorro de batería.',
              style: theme.textTheme.bodyMedium?.copyWith(
                color: colorScheme.onSurfaceVariant,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 20),
            // Info item
            _PermissionInfoItem(
              icon: Icons.flash_on_rounded,
              title: 'Ejecución en segundo plano',
              description: 'Evita que Android detenga el monitoreo',
              theme: theme,
              colorScheme: colorScheme,
            ),
            const SizedBox(height: 24),
            // Botones
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: () => Navigator.pop(context, true),
                style: FilledButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
                  ),
                ),
                child: const Text('Activar'),
              ),
            ),
            const SizedBox(height: 10),
            SizedBox(
              width: double.infinity,
              child: TextButton(
                onPressed: () => Navigator.pop(context, false),
                style: TextButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
                  ),
                ),
                child: Text(
                  'Más tarde',
                  style: TextStyle(
                    color: colorScheme.onSurfaceVariant,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    ) ?? false;

    if (shouldRequest) {
      await PermissionService.requestBatteryOptimizationExemption();
    }
  }

  Future<void> _showPermissionsDialog() async {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    
    final shouldRequestPermissions = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppTheme.radiusLarge),
        ),
        contentPadding: const EdgeInsets.fromLTRB(24, 20, 24, 24),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Icono
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: colorScheme.primaryContainer,
                shape: BoxShape.circle,
              ),
              child: Icon(
                Icons.security_rounded,
                size: 40,
                color: colorScheme.onPrimaryContainer,
              ),
            ),
            const SizedBox(height: 20),
            // Título
            Text(
              'Permisos necesarios',
              style: theme.textTheme.headlineSmall?.copyWith(
                fontWeight: FontWeight.w600,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 12),
            // Descripción
            Text(
              'NetFlow necesita los siguientes permisos para monitorear tu uso de datos.',
              style: theme.textTheme.bodyMedium?.copyWith(
                color: colorScheme.onSurfaceVariant,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 20),
            // Items de permisos
            _PermissionInfoItem(
              icon: Icons.phone_android_rounded,
              title: 'Estado del teléfono',
              description: 'Para monitorear datos móviles',
              theme: theme,
              colorScheme: colorScheme,
            ),
            const SizedBox(height: 10),
            _PermissionInfoItem(
              icon: Icons.notifications_rounded,
              title: 'Notificaciones',
              description: 'Para mostrar velocidad en tiempo real',
              theme: theme,
              colorScheme: colorScheme,
            ),
            const SizedBox(height: 24),
            // Botón único
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: () => Navigator.pop(context, true),
                style: FilledButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
                  ),
                ),
                child: const Text('Otorgar permisos'),
              ),
            ),
          ],
        ),
      ),
    ) ?? false;

    if (!mounted) return;

    if (shouldRequestPermissions) {
      // Solicitar permisos reales
      final granted = await PermissionService.requestAllPermissions();
      
      if (!granted && mounted) {
        // Mostrar snackbar si no se otorgaron permisos
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: const Text('Algunos permisos no fueron otorgados. La app puede no funcionar correctamente.'),
            behavior: SnackBarBehavior.floating,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
            ),
            action: SnackBarAction(
              label: 'Configuración',
              onPressed: () => PermissionService.openSettings(),
            ),
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Scaffold(
      appBar: AppBar(
        title: const Text('NetFlow'),
        actions: [
          // Botón de historial con estilo expresivo
          IconButton(
            icon: Icon(
              Icons.history_rounded,
              color: colorScheme.onSurfaceVariant,
            ),
            tooltip: 'Historial',
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (context) => const HistoryScreen()),
              );
            },
          ),
          // Botón de configuración con estilo expresivo
          IconButton(
            icon: Icon(
              Icons.settings_rounded,
              color: colorScheme.onSurfaceVariant,
            ),
            tooltip: 'Configuración',
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (context) => const SettingsScreen()),
              );
            },
          ),
          const SizedBox(width: 4),
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Estado de conexión - Hero moment expresivo
              _ConnectionStatusHero(theme: theme, colorScheme: colorScheme),
              const SizedBox(height: 28),
              
              // Velocidad en tiempo real
              _SpeedCard(theme: theme, colorScheme: colorScheme),
              const SizedBox(height: 16),
              
              // Uso del día
              _TodayUsageCard(theme: theme, colorScheme: colorScheme),
              const SizedBox(height: 16),
            ],
          ),
        ),
      ),
    );
  }
}

/// Widget Hero que muestra el estado de conexión con estilo expresivo
class _ConnectionStatusHero extends StatelessWidget {
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _ConnectionStatusHero({
    required this.theme,
    required this.colorScheme,
  });

  @override
  Widget build(BuildContext context) {
    return Consumer<NetworkMonitorProvider>(
      builder: (context, provider, child) {
        final networkType = provider.currentNetworkType;
        final isConnected = networkType.isConnected;
        final isMonitoring = provider.isMonitoring;
        
        // Colores expresivos según el tipo de red
        final Color primaryColor;
        final Color containerColor;
        final IconData networkIcon;
        
        switch (networkType) {
          case NetworkType.wifi:
            primaryColor = colorScheme.primary;
            containerColor = colorScheme.primaryContainer;
            networkIcon = Icons.wifi_rounded;
          case NetworkType.mobile:
            primaryColor = colorScheme.tertiary;
            containerColor = colorScheme.tertiaryContainer;
            networkIcon = Icons.signal_cellular_alt_rounded;
          case NetworkType.none:
            primaryColor = colorScheme.error;
            containerColor = colorScheme.errorContainer;
            networkIcon = Icons.signal_cellular_off_rounded;
        }

        return Container(
          padding: const EdgeInsets.all(24),
          decoration: BoxDecoration(
            color: containerColor,
            borderRadius: BorderRadius.circular(AppTheme.radiusLarge),
          ),
          child: Column(
            children: [
              // Icono grande expresivo
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: primaryColor.withValues(alpha: 0.15),
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  networkIcon,
                  size: 48,
                  color: primaryColor,
                ),
              ),
              const SizedBox(height: 16),
              // Estado de conexión
              Text(
                networkType.displayName,
                style: theme.textTheme.headlineSmall?.copyWith(
                  color: primaryColor,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 8),
              // Badge de estado
              AnimatedContainer(
                duration: const Duration(milliseconds: 300),
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                decoration: BoxDecoration(
                  color: isMonitoring 
                      ? colorScheme.primary.withValues(alpha: 0.15)
                      : colorScheme.surfaceContainerHighest,
                  borderRadius: BorderRadius.circular(AppTheme.radiusFull),
                  border: Border.all(
                    color: isMonitoring 
                        ? colorScheme.primary.withValues(alpha: 0.3)
                        : colorScheme.outline.withValues(alpha: 0.2),
                  ),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    if (isMonitoring && isConnected) ...[
                      Container(
                        width: 10,
                        height: 10,
                        decoration: BoxDecoration(
                          color: colorScheme.primary,
                          shape: BoxShape.circle,
                          boxShadow: [
                            BoxShadow(
                              color: colorScheme.primary.withValues(alpha: 0.5),
                              blurRadius: 8,
                              spreadRadius: 1,
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 10),
                    ],
                    Text(
                      isMonitoring ? 'Monitoreando' : 'Detenido',
                      style: theme.textTheme.labelLarge?.copyWith(
                        color: isMonitoring 
                            ? colorScheme.primary 
                            : colorScheme.onSurfaceVariant,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

/// Card con velocidad de bajada/subida en tiempo real - Estilo expresivo
class _SpeedCard extends StatelessWidget {
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _SpeedCard({
    required this.theme,
    required this.colorScheme,
  });

  @override
  Widget build(BuildContext context) {
    return Consumer<NetworkMonitorProvider>(
      builder: (context, provider, child) {
        return Card(
          child: Padding(
            padding: const EdgeInsets.all(28),
            child: Column(
              children: [
                // Título con estilo expresivo
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: colorScheme.secondaryContainer,
                        borderRadius: BorderRadius.circular(AppTheme.radiusSmall),
                      ),
                      child: Icon(
                        Icons.speed_rounded,
                        color: colorScheme.onSecondaryContainer,
                        size: 20,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Text(
                      'Velocidad actual',
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 28),
                // Indicadores de velocidad
                Row(
                  children: [
                    // Bajada
                    Expanded(
                      child: _SpeedIndicator(
                        icon: Icons.arrow_downward_rounded,
                        label: 'Bajada',
                        speed: provider.downloadSpeed,
                        color: colorScheme.error,
                        theme: theme,
                        colorScheme: colorScheme,
                      ),
                    ),
                    // Separador expresivo
                    Container(
                      width: 2,
                      height: 90,
                      margin: const EdgeInsets.symmetric(horizontal: 16),
                      decoration: BoxDecoration(
                        color: colorScheme.outlineVariant.withValues(alpha: 0.5),
                        borderRadius: BorderRadius.circular(1),
                      ),
                    ),
                    // Subida
                    Expanded(
                      child: _SpeedIndicator(
                        icon: Icons.arrow_upward_rounded,
                        label: 'Subida',
                        speed: provider.uploadSpeed,
                        color: colorScheme.primary,
                        theme: theme,
                        colorScheme: colorScheme,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

/// Indicador individual de velocidad con estilo expresivo
class _SpeedIndicator extends StatelessWidget {
  final IconData icon;
  final String label;
  final int speed;
  final Color color;
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _SpeedIndicator({
    required this.icon,
    required this.label,
    required this.speed,
    required this.color,
    required this.theme,
    required this.colorScheme,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // Icono con fondo expresivo
        Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: color.withValues(alpha: 0.12),
            shape: BoxShape.circle,
          ),
          child: Icon(icon, color: color, size: 24),
        ),
        const SizedBox(height: 12),
        // Valor grande (hero moment)
        Text(
          DataFormatter.formatBytesValue(speed),
          style: theme.textTheme.headlineMedium?.copyWith(
            fontWeight: FontWeight.w700,
            color: color,
            letterSpacing: -0.5,
          ),
        ),
        // Unidad
        Text(
          '${DataFormatter.getUnit(speed)}/s',
          style: theme.textTheme.bodyMedium?.copyWith(
            color: colorScheme.onSurfaceVariant,
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(height: 6),
        // Label
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
          decoration: BoxDecoration(
            color: colorScheme.surfaceContainerHighest,
            borderRadius: BorderRadius.circular(AppTheme.radiusFull),
          ),
          child: Text(
            label,
            style: theme.textTheme.labelSmall?.copyWith(
              color: colorScheme.onSurfaceVariant,
              fontWeight: FontWeight.w500,
            ),
          ),
        ),
      ],
    );
  }
}

/// Card con el uso de datos del día - Estilo expresivo
class _TodayUsageCard extends StatelessWidget {
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _TodayUsageCard({
    required this.theme,
    required this.colorScheme,
  });

  @override
  Widget build(BuildContext context) {
    return Consumer<NetworkMonitorProvider>(
      builder: (context, provider, child) {
        final todayDownload = provider.todayDownload;
        final todayUpload = provider.todayUpload;
        final totalToday = todayDownload + todayUpload;

        return Card(
          child: Padding(
            padding: const EdgeInsets.all(28),
            child: Column(
              children: [
                // Header expresivo
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(8),
                          decoration: BoxDecoration(
                            color: colorScheme.primaryContainer,
                            borderRadius: BorderRadius.circular(AppTheme.radiusSmall),
                          ),
                          child: Icon(
                            Icons.data_usage_rounded,
                            color: colorScheme.onPrimaryContainer,
                            size: 20,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Text(
                          'Uso de hoy',
                          style: theme.textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ],
                    ),
                    // Total destacado
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                      decoration: BoxDecoration(
                        color: colorScheme.primary,
                        borderRadius: BorderRadius.circular(AppTheme.radiusFull),
                      ),
                      child: Text(
                        DataFormatter.formatBytes(totalToday),
                        style: theme.textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w700,
                          color: colorScheme.onPrimary,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 24),
                // Barra de progreso expresiva
                ClipRRect(
                  borderRadius: BorderRadius.circular(AppTheme.radiusFull),
                  child: LinearProgressIndicator(
                    value: totalToday > 0 ? todayDownload / totalToday : 0.5,
                    minHeight: 12,
                    backgroundColor: colorScheme.primaryContainer.withValues(alpha: 0.5),
                    valueColor: AlwaysStoppedAnimation(colorScheme.error),
                  ),
                ),
                const SizedBox(height: 20),
                // Detalles de uso
                Row(
                  children: [
                    Expanded(
                      child: _UsageDetailExpressive(
                        icon: Icons.arrow_downward_rounded,
                        label: 'Bajada',
                        value: todayDownload,
                        color: colorScheme.error,
                        theme: theme,
                        colorScheme: colorScheme,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: _UsageDetailExpressive(
                        icon: Icons.arrow_upward_rounded,
                        label: 'Subida',
                        value: todayUpload,
                        color: colorScheme.primary,
                        theme: theme,
                        colorScheme: colorScheme,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

/// Detalle de uso individual con estilo expresivo
class _UsageDetailExpressive extends StatelessWidget {
  final IconData icon;
  final String label;
  final int value;
  final Color color;
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _UsageDetailExpressive({
    required this.icon,
    required this.label,
    required this.value,
    required this.color,
    required this.theme,
    required this.colorScheme,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
        border: Border.all(
          color: color.withValues(alpha: 0.2),
        ),
      ),
      child: Row(
        children: [
          Icon(icon, color: color, size: 20),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: colorScheme.onSurfaceVariant,
                  ),
                ),
                Text(
                  DataFormatter.formatBytes(value),
                  style: theme.textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.w700,
                    color: color,
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

/// Widget para mostrar información de un permiso en el diálogo inicial
class _PermissionInfoItem extends StatelessWidget {
  final IconData icon;
  final String title;
  final String description;
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _PermissionInfoItem({
    required this.icon,
    required this.title,
    required this.description,
    required this.theme,
    required this.colorScheme,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(10),
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
          const SizedBox(width: 14),
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
                const SizedBox(height: 2),
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
