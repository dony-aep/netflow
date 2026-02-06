import 'package:flutter/material.dart';
import 'package:package_info_plus/package_info_plus.dart';

import '../services/github_update_service.dart';
import '../theme/app_theme.dart';

/// Pantalla de actualizaciones mediante GitHub
/// Diseño: Material Design 3 Expressive
class UpdateScreen extends StatefulWidget {
  const UpdateScreen({super.key});

  @override
  State<UpdateScreen> createState() => _UpdateScreenState();
}

class _UpdateScreenState extends State<UpdateScreen> {
  PackageInfo? _packageInfo;
  bool _isChecking = false;
  bool _isDownloading = false;
  double _downloadProgress = 0;
  UpdateStatus _status = UpdateStatus.idle;
  String? _latestVersion;
  String? _releaseNotes;
  String? _downloadUrl;
  String? _errorMessage;
  String? _apkSize;

  @override
  void initState() {
    super.initState();
    _loadPackageInfo();
  }

  Future<void> _loadPackageInfo() async {
    final info = await PackageInfo.fromPlatform();
    if (mounted) {
      setState(() => _packageInfo = info);
    }
  }

  Future<void> _checkForUpdates() async {
    if (_packageInfo == null) return;
    
    setState(() {
      _isChecking = true;
      _status = UpdateStatus.checking;
      _errorMessage = null;
    });

    final result = await GitHubUpdateService.checkForUpdates(_packageInfo!.version);

    if (mounted) {
      setState(() {
        _isChecking = false;
        
        switch (result.status) {
          case UpdateCheckStatus.upToDate:
            _status = UpdateStatus.upToDate;
            _latestVersion = result.releaseInfo?.version;
          case UpdateCheckStatus.updateAvailable:
            _status = UpdateStatus.updateAvailable;
            _latestVersion = result.releaseInfo?.version;
            _releaseNotes = result.releaseInfo?.releaseNotes;
            _downloadUrl = result.releaseInfo?.apkDownloadUrl;
            _apkSize = result.releaseInfo?.apkSizeFormatted;
          case UpdateCheckStatus.error:
            _status = UpdateStatus.error;
            _errorMessage = result.message;
        }
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final currentVersion = _packageInfo?.version ?? '...';

    return Scaffold(
      appBar: AppBar(title: const Text('Actualizaciones')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          children: [
            const SizedBox(height: 32),

            // Icono de estado
            _StatusIcon(
              status: _status,
              isChecking: _isChecking,
              colorScheme: colorScheme,
            ),
            const SizedBox(height: 32),

            // Versión actual
            Text(
              'Versión actual',
              style: theme.textTheme.titleMedium?.copyWith(
                color: colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'v$currentVersion',
              style: theme.textTheme.headlineMedium?.copyWith(
                fontWeight: FontWeight.w700,
                color: colorScheme.primary,
              ),
            ),
            const SizedBox(height: 32),

            // Mensaje de estado
            _StatusMessage(
              status: _status,
              latestVersion: _latestVersion,
              errorMessage: _errorMessage,
              apkSize: _apkSize,
              theme: theme,
              colorScheme: colorScheme,
            ),
            const SizedBox(height: 32),

            // Notas de la versión (si hay actualización)
            if (_status == UpdateStatus.updateAvailable &&
                _releaseNotes != null)
              _ReleaseNotesCard(
                releaseNotes: _releaseNotes!,
                theme: theme,
                colorScheme: colorScheme,
              ),

            if (_status == UpdateStatus.updateAvailable &&
                _releaseNotes != null)
              const SizedBox(height: 24),

            // Botones de acción
            _ActionButtons(
              status: _status,
              isChecking: _isChecking,
              isDownloading: _isDownloading,
              downloadProgress: _downloadProgress,
              onCheck: _checkForUpdates,
              onDownload: _downloadUpdate,
              onOpenInBrowser: _openInBrowser,
              colorScheme: colorScheme,
            ),
            const SizedBox(height: 24),

            // Información adicional
            _InfoSection(theme: theme, colorScheme: colorScheme),
          ],
        ),
      ),
    );
  }

  Future<void> _downloadUpdate() async {
    if (_downloadUrl == null) {
      // Si no hay URL de APK, abrir en navegador
      await _openInBrowser();
      return;
    }

    setState(() {
      _isDownloading = true;
      _downloadProgress = 0;
    });

    final filePath = await GitHubUpdateService.downloadApk(
      onProgress: (progress) {
        if (mounted) {
          setState(() => _downloadProgress = progress);
        }
      },
    );

    if (mounted) {
      setState(() => _isDownloading = false);

      if (filePath != null) {
        // Mostrar diálogo para instalar
        _showInstallDialog(filePath);
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Error al descargar. Intenta desde el navegador.'),
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    }
  }
  
  Future<void> _openInBrowser() async {
    final opened = await GitHubUpdateService.openDownloadInBrowser();
    if (!opened && mounted) {
      // Si no pudo abrir el enlace directo, abrir la página de releases
      await GitHubUpdateService.openReleasesPage();
    }
  }
  
  void _showInstallDialog(String filePath) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Descarga completada'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('El APK se ha descargado correctamente.'),
            const SizedBox(height: 8),
            Text(
              'Ubicación: $filePath',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 16),
            const Text('Para instalar, busca el archivo en tu gestor de archivos.'),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Entendido'),
          ),
        ],
      ),
    );
  }
}

/// Estados posibles de actualización
enum UpdateStatus { idle, checking, upToDate, updateAvailable, error }

/// Icono de estado animado
class _StatusIcon extends StatelessWidget {
  final UpdateStatus status;
  final bool isChecking;
  final ColorScheme colorScheme;

  const _StatusIcon({
    required this.status,
    required this.isChecking,
    required this.colorScheme,
  });

  @override
  Widget build(BuildContext context) {
    final IconData icon;
    final Color iconColor;
    final Color backgroundColor;

    switch (status) {
      case UpdateStatus.idle:
        icon = Icons.system_update_rounded;
        iconColor = colorScheme.primary;
        backgroundColor = colorScheme.primaryContainer;
      case UpdateStatus.checking:
        icon = Icons.sync_rounded;
        iconColor = colorScheme.primary;
        backgroundColor = colorScheme.primaryContainer;
      case UpdateStatus.upToDate:
        icon = Icons.check_circle_rounded;
        iconColor = colorScheme.primary;
        backgroundColor = colorScheme.primaryContainer;
      case UpdateStatus.updateAvailable:
        icon = Icons.download_rounded;
        iconColor = colorScheme.tertiary;
        backgroundColor = colorScheme.tertiaryContainer;
      case UpdateStatus.error:
        icon = Icons.error_rounded;
        iconColor = colorScheme.error;
        backgroundColor = colorScheme.errorContainer;
    }

    return Container(
      width: 100,
      height: 100,
      decoration: BoxDecoration(
        color: backgroundColor,
        shape: BoxShape.circle,
      ),
      child: isChecking
          ? Padding(
              padding: const EdgeInsets.all(28),
              child: CircularProgressIndicator(
                strokeWidth: 3,
                color: iconColor,
              ),
            )
          : Icon(icon, size: 48, color: iconColor),
    );
  }
}

/// Mensaje de estado
class _StatusMessage extends StatelessWidget {
  final UpdateStatus status;
  final String? latestVersion;
  final String? errorMessage;
  final String? apkSize;
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _StatusMessage({
    required this.status,
    required this.latestVersion,
    required this.errorMessage,
    this.apkSize,
    required this.theme,
    required this.colorScheme,
  });

  @override
  Widget build(BuildContext context) {
    final String title;
    final String subtitle;
    final Color titleColor;

    switch (status) {
      case UpdateStatus.idle:
        title = 'Verificar actualizaciones';
        subtitle = 'Toca el botón para buscar nuevas versiones';
        titleColor = colorScheme.onSurface;
      case UpdateStatus.checking:
        title = 'Buscando...';
        subtitle = 'Conectando con GitHub';
        titleColor = colorScheme.primary;
      case UpdateStatus.upToDate:
        title = '¡Estás actualizado!';
        subtitle = 'Tienes la última versión disponible';
        titleColor = colorScheme.primary;
      case UpdateStatus.updateAvailable:
        title = 'Nueva versión disponible';
        final sizeInfo = apkSize != null && apkSize!.isNotEmpty ? ' ($apkSize)' : '';
        subtitle = latestVersion != null
            ? 'Versión $latestVersion lista para descargar$sizeInfo'
            : 'Hay una actualización disponible$sizeInfo';
        titleColor = colorScheme.tertiary;
      case UpdateStatus.error:
        title = 'Error';
        subtitle = errorMessage ?? 'No se pudo verificar actualizaciones';
        titleColor = colorScheme.error;
    }

    return Column(
      children: [
        Text(
          title,
          style: theme.textTheme.titleLarge?.copyWith(
            fontWeight: FontWeight.w600,
            color: titleColor,
          ),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 8),
        Text(
          subtitle,
          style: theme.textTheme.bodyMedium?.copyWith(
            color: colorScheme.onSurfaceVariant,
          ),
          textAlign: TextAlign.center,
        ),
      ],
    );
  }
}

/// Tarjeta de notas de versión
class _ReleaseNotesCard extends StatelessWidget {
  final String releaseNotes;
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _ReleaseNotesCard({
    required this.releaseNotes,
    required this.theme,
    required this.colorScheme,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(
                Icons.description_rounded,
                size: 20,
                color: colorScheme.primary,
              ),
              const SizedBox(width: 8),
              Text(
                'Novedades',
                style: theme.textTheme.titleSmall?.copyWith(
                  fontWeight: FontWeight.w600,
                  color: colorScheme.onSurface,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            releaseNotes,
            style: theme.textTheme.bodySmall?.copyWith(
              color: colorScheme.onSurfaceVariant,
              height: 1.5,
            ),
          ),
        ],
      ),
    );
  }
}

/// Botones de acción
class _ActionButtons extends StatelessWidget {
  final UpdateStatus status;
  final bool isChecking;
  final bool isDownloading;
  final double downloadProgress;
  final VoidCallback onCheck;
  final VoidCallback onDownload;
  final VoidCallback onOpenInBrowser;
  final ColorScheme colorScheme;

  const _ActionButtons({
    required this.status,
    required this.isChecking,
    this.isDownloading = false,
    this.downloadProgress = 0,
    required this.onCheck,
    required this.onDownload,
    required this.onOpenInBrowser,
    required this.colorScheme,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // Indicador de progreso de descarga
        if (isDownloading) ...[
          ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: LinearProgressIndicator(
              value: downloadProgress,
              minHeight: 8,
              backgroundColor: colorScheme.surfaceContainerHighest,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'Descargando... ${(downloadProgress * 100).toStringAsFixed(0)}%',
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 16),
        ],
        
        // Botón principal
        SizedBox(
          width: double.infinity,
          child: FilledButton.icon(
            onPressed: isChecking || isDownloading
                ? null
                : (status == UpdateStatus.updateAvailable
                      ? onDownload
                      : onCheck),
            icon: Icon(
              status == UpdateStatus.updateAvailable
                  ? Icons.download_rounded
                  : Icons.refresh_rounded,
            ),
            label: Text(
              isDownloading
                  ? 'Descargando...'
                  : status == UpdateStatus.updateAvailable
                      ? 'Descargar actualización'
                      : 'Buscar actualizaciones',
            ),
            style: FilledButton.styleFrom(
              padding: const EdgeInsets.symmetric(vertical: 16),
              backgroundColor: status == UpdateStatus.updateAvailable
                  ? colorScheme.tertiary
                  : null,
            ),
          ),
        ),

        // Botones secundarios si hay actualización
        if (status == UpdateStatus.updateAvailable) ...[
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: isDownloading ? null : onOpenInBrowser,
                  icon: const Icon(Icons.open_in_browser_rounded),
                  label: const Text('Abrir en navegador'),
                  style: OutlinedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 16),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: isDownloading ? null : onCheck,
                  icon: const Icon(Icons.refresh_rounded),
                  label: const Text('Verificar'),
                  style: OutlinedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 16),
                  ),
                ),
              ),
            ],
          ),
        ],
      ],
    );
  }
}

/// Sección de información
class _InfoSection extends StatelessWidget {
  final ThemeData theme;
  final ColorScheme colorScheme;

  const _InfoSection({required this.theme, required this.colorScheme});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.3),
        borderRadius: BorderRadius.circular(AppTheme.radiusMedium),
        border: Border.all(
          color: colorScheme.outlineVariant.withValues(alpha: 0.3),
        ),
      ),
      child: Row(
        children: [
          Icon(
            Icons.info_outline_rounded,
            size: 20,
            color: colorScheme.onSurfaceVariant,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              'Las actualizaciones se descargan directamente desde GitHub Releases',
              style: theme.textTheme.bodySmall?.copyWith(
                color: colorScheme.onSurfaceVariant,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
