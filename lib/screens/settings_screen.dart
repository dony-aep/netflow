import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:provider/provider.dart';

import '../services/services.dart';
import '../providers/providers.dart';
import '../theme/app_theme.dart';
import 'about_screen.dart';
import 'advanced_settings_screen.dart';
import 'update_screen.dart';

/// Pantalla de configuración de la aplicación
/// Diseño: Material Design 3 Expressive
class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

/// Unidades de datos disponibles
enum DataUnit {
  kb,
  mb,
  gb;

  /// Nombre para mostrar en UI
  String get displayName => name.toUpperCase();
}

/// Unidades de velocidad disponibles
enum SpeedUnit {
  bitsPerSecond,
  bytesPerSecond;

  String get displayName {
    switch (this) {
      case SpeedUnit.bitsPerSecond:
        return 'Bits/s (Kb/s, Mb/s)';
      case SpeedUnit.bytesPerSecond:
        return 'Bytes/s (KB/s, MB/s)';
    }
  }

  String get shortName {
    switch (this) {
      case SpeedUnit.bitsPerSecond:
        return 'b/s';
      case SpeedUnit.bytesPerSecond:
        return 'B/s';
    }
  }
}

class _SettingsScreenState extends State<SettingsScreen> {
  // Preferencias de datos
  double _dataLimitValue = 0;
  DataUnit _dataLimitUnit = DataUnit.gb;
  bool _dataLimitEnabled = false;
  int _billingCycleDay = 1; // Día de inicio del ciclo (1-28)

  // Preferencias de notificación
  bool _hideOnLockscreen = false;
  SpeedUnit _speedUnit = SpeedUnit.bytesPerSecond;

  @override
  void initState() {
    super.initState();
    _loadPreferences();
    // Verificar permisos al entrar a configuración
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _checkNotificationPermission();
    });
  }

  Future<void> _checkNotificationPermission() async {
    final hasPermission = await PermissionService.hasNotificationPermission();

    if (!hasPermission && mounted) {
      _showPermissionRequiredDialog();
    }
  }

  Future<void> _showPermissionRequiredDialog() async {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    final shouldOpenSettings = await showDialog<bool>(
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
            // Icono de advertencia
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: colorScheme.errorContainer,
                shape: BoxShape.circle,
              ),
              child: Icon(
                Icons.notifications_off_rounded,
                size: 32,
                color: colorScheme.onErrorContainer,
              ),
            ),
            const SizedBox(height: 16),
            // Título
            Text(
              'Notificaciones desactivadas',
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w600,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 12),
            // Descripción
            Text(
              'Las notificaciones están desactivadas. Para ver la velocidad de red en tiempo real, actívalas desde la configuración del sistema.',
              style: theme.textTheme.bodyMedium?.copyWith(
                color: colorScheme.onSurfaceVariant,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 24),
            // Botones
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => Navigator.pop(context, false),
                    style: OutlinedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(
                          AppTheme.radiusMedium,
                        ),
                      ),
                    ),
                    child: const Text('Más tarde'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: FilledButton(
                    onPressed: () => Navigator.pop(context, true),
                    style: FilledButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(
                          AppTheme.radiusMedium,
                        ),
                      ),
                    ),
                    child: const Text('Activar'),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );

    if (shouldOpenSettings == true && mounted) {
      await PermissionService.openSettings();
    }
  }

  Future<void> _loadPreferences() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      // Datos
      _dataLimitValue = prefs.getDouble('dataLimitValue') ?? 0;
      _dataLimitUnit = DataUnit.values[prefs.getInt('dataLimitUnit') ?? 2];
      _dataLimitEnabled = prefs.getBool('dataLimitEnabled') ?? false;
      _billingCycleDay = prefs.getInt('billingCycleDay') ?? 1;
      // Notificación
      _hideOnLockscreen = prefs.getBool('hideOnLockscreen') ?? false;
      _speedUnit =
          SpeedUnit.values[prefs.getInt('speedUnit') ?? 1]; // Default: Bytes/s
    });
  }

  void _showSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), behavior: SnackBarBehavior.floating),
    );
  }

  String get _speedUnitLabel {
    return _speedUnit == SpeedUnit.bitsPerSecond ? 'Bits/s' : 'Bytes/s';
  }

  /// Formatea el límite para mostrar
  String get _formattedLimit {
    if (_dataLimitValue == _dataLimitValue.toInt()) {
      return '${_dataLimitValue.toInt()} ${_dataLimitUnit.displayName}';
    }
    return '${_dataLimitValue.toStringAsFixed(1)} ${_dataLimitUnit.displayName}';
  }

  Future<void> _savePreference(String key, dynamic value) async {
    final prefs = await SharedPreferences.getInstance();
    if (value is bool) {
      await prefs.setBool(key, value);
    } else if (value is int) {
      await prefs.setInt(key, value);
    } else if (value is double) {
      await prefs.setDouble(key, value);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Scaffold(
      appBar: AppBar(title: const Text('Configuración')),
      body: ListView(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        children: [
          // Sección: Notificación
          _SettingsSection(
            title: 'Notificación',
            theme: theme,
            children: [
              _ExpressiveSwitchTile(
                title: 'Ocultar en pantalla de bloqueo',
                subtitle: _hideOnLockscreen
                    ? 'Notificación oculta en lockscreen'
                    : 'Notificación visible en lockscreen',
                value: _hideOnLockscreen,
                icon: Icons.lock_rounded,
                iconColor: colorScheme.secondary,
                onChanged: (value) async {
                  setState(() => _hideOnLockscreen = value);
                  await _savePreference('hideOnLockscreen', value);

                  // Reiniciar servicio para aplicar el cambio
                  if (await NetworkMonitorService.isRunning) {
                    await NetworkMonitorService.restart();
                    if (mounted) {
                      _showSnackBar(
                        value
                            ? 'Notificación oculta en pantalla de bloqueo'
                            : 'Notificación visible en pantalla de bloqueo',
                      );
                    }
                  }
                },
                theme: theme,
                colorScheme: colorScheme,
              ),
              const SizedBox(height: 8),
              _ExpressiveListTile(
                title: 'Unidad de velocidad',
                subtitle: _speedUnitLabel,
                icon: Icons.speed_rounded,
                iconColor: colorScheme.secondary,
                onTap: () => _showSpeedUnitPicker(context),
                theme: theme,
                colorScheme: colorScheme,
              ),
            ],
          ),
          const SizedBox(height: 12),

          // Sección: Datos
          _SettingsSection(
            title: 'Datos',
            theme: theme,
            children: [
              _ExpressiveSwitchTile(
                title: 'Límite de datos',
                subtitle: _dataLimitEnabled
                    ? 'Límite: $_formattedLimit'
                    : 'Sin límite configurado',
                value: _dataLimitEnabled,
                icon: Icons.data_usage_rounded,
                iconColor: colorScheme.error,
                onChanged: (value) async {
                  setState(() => _dataLimitEnabled = value);
                  await _savePreference('dataLimitEnabled', value);
                  NetworkMonitorService.reloadDataLimitConfig();
                  if (value && _dataLimitValue == 0 && context.mounted) {
                    _showDataLimitPicker(context);
                  }
                },
                theme: theme,
                colorScheme: colorScheme,
              ),
              if (_dataLimitEnabled) ...[
                const SizedBox(height: 8),
                _ExpressiveListTile(
                  title: 'Configurar límite',
                  subtitle: '$_formattedLimit • Día $_billingCycleDay del mes',
                  icon: Icons.tune_rounded,
                  iconColor: colorScheme.error,
                  onTap: () => _showDataLimitPicker(context),
                  theme: theme,
                  colorScheme: colorScheme,
                ),
              ],
              const SizedBox(height: 8),
              _ExpressiveListTile(
                title: 'Limpiar historial',
                subtitle: 'Eliminar todos los datos guardados',
                icon: Icons.delete_forever_rounded,
                iconColor: colorScheme.error,
                isDestructive: true,
                onTap: () => _showClearDataDialog(context),
                theme: theme,
                colorScheme: colorScheme,
              ),
            ],
          ),
          const SizedBox(height: 12),

          // Sección: Apariencia
          _SettingsSection(
            title: 'Apariencia',
            theme: theme,
            children: [_buildThemeTile(context, theme, colorScheme)],
          ),
          const SizedBox(height: 12),

          // Sección: Avanzado
          _SettingsSection(
            title: 'Avanzado',
            theme: theme,
            children: [
              _ExpressiveListTile(
                title: 'Opciones avanzadas',
                subtitle: 'Permisos y optimización del sistema',
                icon: Icons.tune_rounded,
                iconColor: colorScheme.tertiary,
                onTap: () => Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (context) => const AdvancedSettingsScreen(),
                  ),
                ),
                theme: theme,
                colorScheme: colorScheme,
              ),
            ],
          ),
          const SizedBox(height: 12),

          // Sección: Información
          _SettingsSection(
            title: 'Información',
            theme: theme,
            children: [
              _ExpressiveListTile(
                title: 'Buscar actualizaciones',
                subtitle: 'Verificar nuevas versiones en GitHub',
                icon: Icons.system_update_rounded,
                iconColor: colorScheme.tertiary,
                onTap: () => Navigator.push(
                  context,
                  MaterialPageRoute(builder: (context) => const UpdateScreen()),
                ),
                theme: theme,
                colorScheme: colorScheme,
              ),
              const SizedBox(height: 8),
              _ExpressiveListTile(
                title: 'Acerca de NetFlow',
                subtitle: 'Información y versión de la aplicación',
                icon: Icons.favorite_rounded,
                iconColor: colorScheme.error,
                onTap: () => Navigator.push(
                  context,
                  MaterialPageRoute(builder: (context) => const AboutScreen()),
                ),
                theme: theme,
                colorScheme: colorScheme,
              ),
            ],
          ),
          const SizedBox(height: 24),
        ],
      ),
    );
  }

  Widget _buildThemeTile(
    BuildContext context,
    ThemeData theme,
    ColorScheme colorScheme,
  ) {
    return Consumer<ThemeProvider>(
      builder: (context, themeProvider, child) {
        return _ExpressiveListTile(
          title: 'Tema',
          subtitle: themeProvider.themeModeName,
          icon: themeProvider.themeModeIcon,
          iconColor: colorScheme.primary,
          onTap: () => _showThemePicker(context),
          theme: theme,
          colorScheme: colorScheme,
        );
      },
    );
  }

  void _showThemePicker(BuildContext context) {
    final themeProvider = context.read<ThemeProvider>();

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Seleccionar tema'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            _buildThemeOption(
              context: context,
              icon: Icons.brightness_auto_rounded,
              title: 'Sistema',
              subtitle: 'Usar configuración del dispositivo',
              mode: ThemeMode.system,
              currentMode: themeProvider.themeMode,
              onTap: () {
                themeProvider.setThemeMode(ThemeMode.system);
                Navigator.pop(context);
              },
            ),
            const SizedBox(height: 8),
            _buildThemeOption(
              context: context,
              icon: Icons.light_mode_rounded,
              title: 'Claro',
              subtitle: 'Siempre usar tema claro',
              mode: ThemeMode.light,
              currentMode: themeProvider.themeMode,
              onTap: () {
                themeProvider.setThemeMode(ThemeMode.light);
                Navigator.pop(context);
              },
            ),
            const SizedBox(height: 8),
            _buildThemeOption(
              context: context,
              icon: Icons.dark_mode_rounded,
              title: 'Oscuro',
              subtitle: 'Siempre usar tema oscuro',
              mode: ThemeMode.dark,
              currentMode: themeProvider.themeMode,
              onTap: () {
                themeProvider.setThemeMode(ThemeMode.dark);
                Navigator.pop(context);
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildThemeOption({
    required BuildContext context,
    required IconData icon,
    required String title,
    required String subtitle,
    required ThemeMode mode,
    required ThemeMode currentMode,
    required VoidCallback onTap,
  }) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final isSelected = mode == currentMode;

    return Material(
      color: isSelected
          ? colorScheme.primaryContainer
          : colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
      borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: isSelected
                      ? colorScheme.primary.withValues(alpha: 0.15)
                      : colorScheme.surfaceContainerHighest,
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  icon,
                  color: isSelected
                      ? colorScheme.primary
                      : colorScheme.onSurfaceVariant,
                  size: 24,
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: theme.textTheme.titleSmall?.copyWith(
                        fontWeight: isSelected
                            ? FontWeight.w600
                            : FontWeight.w500,
                        color: isSelected
                            ? colorScheme.onPrimaryContainer
                            : colorScheme.onSurface,
                      ),
                    ),
                    Text(
                      subtitle,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: isSelected
                            ? colorScheme.onPrimaryContainer.withValues(
                                alpha: 0.8,
                              )
                            : colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              if (isSelected)
                Icon(
                  Icons.check_circle_rounded,
                  color: colorScheme.primary,
                  size: 24,
                ),
            ],
          ),
        ),
      ),
    );
  }

  void _showClearDataDialog(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: colorScheme.errorContainer,
                shape: BoxShape.circle,
              ),
              child: Icon(
                Icons.delete_forever_rounded,
                color: colorScheme.error,
                size: 24,
              ),
            ),
            const SizedBox(width: 12),
            const Text('Limpiar historial'),
          ],
        ),
        content: Text(
          'Esta acción eliminará todos los datos de uso guardados y no se puede deshacer.',
          style: theme.textTheme.bodyMedium?.copyWith(
            color: colorScheme.onSurfaceVariant,
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () async {
              Navigator.pop(context);
              await _clearAllData();
            },
            style: FilledButton.styleFrom(
              backgroundColor: colorScheme.error,
              foregroundColor: colorScheme.onError,
            ),
            child: const Text('Eliminar'),
          ),
        ],
      ),
    );
  }

  Future<void> _clearAllData() async {
    try {
      await DatabaseService.clearAllData();

      // Resetear también el uso de hoy en el provider
      if (mounted) {
        final provider = context.read<NetworkMonitorProvider>();
        provider.resetTodayStats();

        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Historial eliminado correctamente'),
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error al eliminar: $e'),
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    }
  }

  void _showDataLimitPicker(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    double tempValue = _dataLimitValue > 0 ? _dataLimitValue : 1.0;
    DataUnit tempUnit = _dataLimitUnit;
    int tempBillingDay = _billingCycleDay;
    final textController = TextEditingController(text: tempValue.toString());

    showDialog(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) {
          // Actualiza el valor formateado
          String getFormattedValue() {
            if (tempValue == tempValue.toInt()) {
              return '${tempValue.toInt()} ${tempUnit.displayName}';
            }
            return '${tempValue.toStringAsFixed(1)} ${tempUnit.displayName}';
          }

          return AlertDialog(
            title: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: colorScheme.errorContainer,
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    Icons.data_usage_rounded,
                    color: colorScheme.error,
                    size: 24,
                  ),
                ),
                const SizedBox(width: 12),
                const Text('Límite de datos'),
              ],
            ),
            content: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  // Vista previa del límite
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.symmetric(vertical: 20),
                    decoration: BoxDecoration(
                      color: colorScheme.errorContainer,
                      borderRadius: BorderRadius.circular(
                        AppTheme.radiusMedium,
                      ),
                    ),
                    child: Column(
                      children: [
                        Text(
                          getFormattedValue(),
                          style: theme.textTheme.headlineMedium?.copyWith(
                            fontWeight: FontWeight.w700,
                            color: colorScheme.error,
                          ),
                        ),
                        Text(
                          'por mes',
                          style: theme.textTheme.bodyMedium?.copyWith(
                            color: colorScheme.onErrorContainer,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 20),
                  // Campo de cantidad y selector de unidad
                  Row(
                    children: [
                      // Campo de texto para cantidad
                      Expanded(
                        flex: 2,
                        child: TextField(
                          controller: textController,
                          keyboardType: const TextInputType.numberWithOptions(
                            decimal: true,
                          ),
                          decoration: InputDecoration(
                            labelText: 'Cantidad',
                            border: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(
                                AppTheme.radiusSmall,
                              ),
                            ),
                            contentPadding: const EdgeInsets.symmetric(
                              horizontal: 12,
                              vertical: 14,
                            ),
                          ),
                          onChanged: (value) {
                            final parsed = double.tryParse(value);
                            if (parsed != null && parsed > 0) {
                              setDialogState(() => tempValue = parsed);
                            }
                          },
                        ),
                      ),
                      const SizedBox(width: 12),
                      // Selector de unidad
                      Expanded(
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 4),
                          decoration: BoxDecoration(
                            border: Border.all(color: colorScheme.outline),
                            borderRadius: BorderRadius.circular(
                              AppTheme.radiusSmall,
                            ),
                          ),
                          child: DropdownButtonHideUnderline(
                            child: DropdownButton<DataUnit>(
                              value: tempUnit,
                              isExpanded: true,
                              items: DataUnit.values
                                  .map<DropdownMenuItem<DataUnit>>((unit) {
                                    return DropdownMenuItem<DataUnit>(
                                      value: unit,
                                      child: Padding(
                                        padding: const EdgeInsets.symmetric(
                                          horizontal: 8,
                                        ),
                                        child: Text(
                                          unit.displayName,
                                          style: theme.textTheme.titleMedium,
                                        ),
                                      ),
                                    );
                                  })
                                  .toList(),
                              onChanged: (value) {
                                if (value != null) {
                                  setDialogState(() => tempUnit = value);
                                }
                              },
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  // Botones de presets rápidos
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: [
                        _PresetChip(
                          label: '500 MB',
                          onTap: () {
                            setDialogState(() {
                              tempValue = 500;
                              tempUnit = DataUnit.mb;
                              textController.text = '500';
                            });
                          },
                          colorScheme: colorScheme,
                        ),
                        const SizedBox(width: 8),
                        _PresetChip(
                          label: '1 GB',
                          onTap: () {
                            setDialogState(() {
                              tempValue = 1;
                              tempUnit = DataUnit.gb;
                              textController.text = '1';
                            });
                          },
                          colorScheme: colorScheme,
                        ),
                        const SizedBox(width: 8),
                        _PresetChip(
                          label: '2 GB',
                          onTap: () {
                            setDialogState(() {
                              tempValue = 2;
                              tempUnit = DataUnit.gb;
                              textController.text = '2';
                            });
                          },
                          colorScheme: colorScheme,
                        ),
                        const SizedBox(width: 8),
                        _PresetChip(
                          label: '5 GB',
                          onTap: () {
                            setDialogState(() {
                              tempValue = 5;
                              tempUnit = DataUnit.gb;
                              textController.text = '5';
                            });
                          },
                          colorScheme: colorScheme,
                        ),
                        const SizedBox(width: 8),
                        _PresetChip(
                          label: '10 GB',
                          onTap: () {
                            setDialogState(() {
                              tempValue = 10;
                              tempUnit = DataUnit.gb;
                              textController.text = '10';
                            });
                          },
                          colorScheme: colorScheme,
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                  // Selector de día de inicio del ciclo
                  Row(
                    children: [
                      Icon(
                        Icons.calendar_today_rounded,
                        color: colorScheme.primary,
                        size: 20,
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          'Día de inicio del ciclo',
                          style: theme.textTheme.bodyMedium,
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 4),
                        decoration: BoxDecoration(
                          border: Border.all(color: colorScheme.outline),
                          borderRadius: BorderRadius.circular(
                            AppTheme.radiusSmall,
                          ),
                        ),
                        child: DropdownButtonHideUnderline(
                          child: DropdownButton<int>(
                            value: tempBillingDay,
                            items: List.generate(28, (index) {
                              final day = index + 1;
                              return DropdownMenuItem<int>(
                                value: day,
                                child: Padding(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 8,
                                  ),
                                  child: Text(
                                    day.toString(),
                                    style: theme.textTheme.titleMedium,
                                  ),
                                ),
                              );
                            }),
                            onChanged: (value) {
                              if (value != null) {
                                setDialogState(() => tempBillingDay = value);
                              }
                            },
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  // Información
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: colorScheme.surfaceContainerHighest.withValues(
                        alpha: 0.5,
                      ),
                      borderRadius: BorderRadius.circular(AppTheme.radiusSmall),
                    ),
                    child: Row(
                      children: [
                        Icon(
                          Icons.notifications_active_rounded,
                          color: colorScheme.secondary,
                          size: 20,
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Text(
                            'Recibirás una notificación al alcanzar este límite',
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: colorScheme.onSurfaceVariant,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('Cancelar'),
              ),
              FilledButton(
                onPressed: () async {
                  if (tempValue > 0) {
                    setState(() {
                      _dataLimitValue = tempValue;
                      _dataLimitUnit = tempUnit;
                      _billingCycleDay = tempBillingDay;
                      _dataLimitEnabled = true;
                    });
                    await _savePreference('dataLimitValue', tempValue);
                    await _savePreference('dataLimitUnit', tempUnit.index);
                    await _savePreference('billingCycleDay', tempBillingDay);
                    await _savePreference('dataLimitEnabled', true);
                    NetworkMonitorService.reloadDataLimitConfig();
                    if (context.mounted) Navigator.pop(context);
                  }
                },
                child: const Text('Guardar'),
              ),
            ],
          );
        },
      ),
    );
  }

  void _showSpeedUnitPicker(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    showDialog(
      context: context,
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
                Icons.speed_rounded,
                color: colorScheme.primary,
                size: 24,
              ),
            ),
            const SizedBox(width: 12),
            const Expanded(child: Text('Unidad de velocidad')),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            _buildSpeedUnitOption(
              context,
              SpeedUnit.bytesPerSecond,
              'Bytes por segundo',
              'KB/s, MB/s - Común en administradores de archivos',
            ),
            const SizedBox(height: 8),
            _buildSpeedUnitOption(
              context,
              SpeedUnit.bitsPerSecond,
              'Bits por segundo',
              'Kbps, Mbps - Común en pruebas de velocidad',
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancelar'),
          ),
        ],
      ),
    );
  }

  Widget _buildSpeedUnitOption(
    BuildContext context,
    SpeedUnit unit,
    String title,
    String subtitle,
  ) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final isSelected = _speedUnit == unit;

    return Material(
      color: isSelected ? colorScheme.primaryContainer : Colors.transparent,
      borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
      child: InkWell(
        onTap: () async {
          // Capturar messenger antes de cualquier async
          final messenger = ScaffoldMessenger.of(context);
          final navigator = Navigator.of(context);
          final message = unit == SpeedUnit.bitsPerSecond
              ? 'Velocidad en bits/s (Kbps, Mbps)'
              : 'Velocidad en bytes/s (KB/s, MB/s)';

          setState(() => _speedUnit = unit);
          await _savePreference('speedUnit', unit.index);

          if (mounted) navigator.pop();

          // Reiniciar servicio para aplicar el cambio en la notificación
          if (await NetworkMonitorService.isRunning) {
            await NetworkMonitorService.restart();
            messenger.showSnackBar(
              SnackBar(
                content: Text(message),
                behavior: SnackBarBehavior.floating,
              ),
            );
          }
        },
        borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: isSelected
                            ? FontWeight.w600
                            : FontWeight.w500,
                        color: isSelected
                            ? colorScheme.onPrimaryContainer
                            : colorScheme.onSurface,
                      ),
                    ),
                    Text(
                      subtitle,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: isSelected
                            ? colorScheme.onPrimaryContainer.withValues(
                                alpha: 0.8,
                              )
                            : colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              if (isSelected)
                Icon(
                  Icons.check_circle_rounded,
                  color: colorScheme.primary,
                  size: 24,
                ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Chip de preset para límite de datos
class _PresetChip extends StatelessWidget {
  final String label;
  final VoidCallback onTap;
  final ColorScheme colorScheme;

  const _PresetChip({
    required this.label,
    required this.onTap,
    required this.colorScheme,
  });

  @override
  Widget build(BuildContext context) {
    return ActionChip(
      label: Text(label, style: TextStyle(color: colorScheme.onSurface)),
      onPressed: onTap,
      backgroundColor: colorScheme.surfaceContainerHighest,
    );
  }
}

/// Sección de configuración con estilo expresivo
class _SettingsSection extends StatelessWidget {
  final String title;
  final ThemeData theme;
  final List<Widget> children;

  const _SettingsSection({
    required this.title,
    required this.theme,
    required this.children,
  });

  @override
  Widget build(BuildContext context) {
    final colorScheme = theme.colorScheme;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Header de la sección
        Padding(
          padding: const EdgeInsets.only(left: 4, bottom: 12),
          child: Text(
            title,
            style: theme.textTheme.titleSmall?.copyWith(
              fontWeight: FontWeight.w600,
              color: colorScheme.primary,
            ),
          ),
        ),
        // Contenido
        ...children,
      ],
    );
  }
}

/// ListTile expresivo para configuración
class _ExpressiveListTile extends StatelessWidget {
  final String title;
  final String subtitle;
  final IconData icon;
  final Color iconColor;
  final VoidCallback onTap;
  final ThemeData theme;
  final ColorScheme colorScheme;
  final bool isDestructive;

  const _ExpressiveListTile({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.iconColor,
    required this.onTap,
    required this.theme,
    required this.colorScheme,
    this.isDestructive = false,
  });

  @override
  Widget build(BuildContext context) {
    // Para ListTile siempre usar color neutro (ya que no tiene estado on/off)
    final tileIconColor = isDestructive
        ? colorScheme.error
        : colorScheme.onSurfaceVariant;
    final tileBgColor = isDestructive
        ? colorScheme.errorContainer
        : colorScheme.surfaceContainerHighest;

    return Material(
      color: isDestructive
          ? colorScheme.errorContainer.withValues(alpha: 0.3)
          : colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
      borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: tileBgColor,
                  shape: BoxShape.circle,
                ),
                child: Icon(icon, color: tileIconColor, size: 22),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: theme.textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w600,
                        color: isDestructive
                            ? colorScheme.error
                            : colorScheme.onSurface,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      subtitle,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              Icon(
                Icons.chevron_right_rounded,
                color: colorScheme.onSurfaceVariant,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// SwitchTile expresivo para configuración
class _ExpressiveSwitchTile extends StatelessWidget {
  final String title;
  final String subtitle;
  final bool value;
  final IconData icon;
  final Color iconColor;
  final ValueChanged<bool> onChanged;
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _ExpressiveSwitchTile({
    required this.title,
    required this.subtitle,
    required this.value,
    required this.icon,
    required this.iconColor,
    required this.onChanged,
    required this.theme,
    required this.colorScheme,
  });

  @override
  Widget build(BuildContext context) {
    // Color activo o neutro según el estado
    final activeColor = value ? iconColor : colorScheme.onSurfaceVariant;
    final bgColor = value
        ? iconColor.withValues(alpha: 0.15)
        : colorScheme.surfaceContainerHighest;

    return Material(
      color: value
          ? colorScheme.primaryContainer.withValues(alpha: 0.4)
          : colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
      borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
      child: InkWell(
        onTap: () => onChanged(!value),
        borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              AnimatedContainer(
                duration: const Duration(milliseconds: 200),
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: bgColor,
                  shape: BoxShape.circle,
                ),
                child: Icon(icon, color: activeColor, size: 22),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: theme.textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w600,
                        color: colorScheme.onSurface,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      subtitle,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              Switch(value: value, onChanged: onChanged),
            ],
          ),
        ),
      ),
    );
  }
}
