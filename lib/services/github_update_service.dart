import 'dart:convert';
import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';
import 'package:url_launcher/url_launcher.dart';

/// Servicio para verificar y descargar actualizaciones desde GitHub Releases
class GitHubUpdateService {
  GitHubUpdateService._();
  
  /// Propietario del repositorio
  static const String _owner = 'dony-aep';
  
  /// Nombre del repositorio
  static const String _repo = 'netflow';
  
  /// URL de la API de GitHub para releases
  static String get _apiUrl => 'https://api.github.com/repos/$_owner/$_repo/releases/latest';
  
  /// Información de la última release disponible
  static ReleaseInfo? _latestRelease;
  
  /// Obtiene la última release disponible
  static ReleaseInfo? get latestRelease => _latestRelease;
  
  /// Verifica si hay una actualización disponible
  /// 
  /// [currentVersion] - Versión actual de la app (ej: "1.0.0")
  /// Retorna [UpdateResult] con el estado de la verificación
  static Future<UpdateResult> checkForUpdates(String currentVersion) async {
    try {
      final response = await http.get(
        Uri.parse(_apiUrl),
        headers: {
          'Accept': 'application/vnd.github.v3+json',
          'User-Agent': 'NetFlow-App',
        },
      );
      
      if (response.statusCode == 200) {
        final data = json.decode(response.body) as Map<String, dynamic>;
        
        // Parsear información de la release
        final tagName = data['tag_name'] as String? ?? '';
        final latestVersion = tagName.replaceFirst('v', '').trim();
        final releaseNotes = data['body'] as String? ?? '';
        final htmlUrl = data['html_url'] as String? ?? '';
        final publishedAt = data['published_at'] as String?;
        
        // Buscar el APK en los assets
        String? apkUrl;
        int? apkSize;
        final assets = data['assets'] as List<dynamic>? ?? [];
        for (final asset in assets) {
          final name = asset['name'] as String? ?? '';
          if (name.endsWith('.apk')) {
            apkUrl = asset['browser_download_url'] as String?;
            apkSize = asset['size'] as int?;
            break;
          }
        }
        
        _latestRelease = ReleaseInfo(
          version: latestVersion,
          tagName: tagName,
          releaseNotes: releaseNotes,
          htmlUrl: htmlUrl,
          apkDownloadUrl: apkUrl,
          apkSize: apkSize,
          publishedAt: publishedAt != null ? DateTime.tryParse(publishedAt) : null,
        );
        
        // Comparar versiones
        final hasUpdate = _compareVersions(currentVersion, latestVersion) < 0;
        
        return UpdateResult(
          status: hasUpdate ? UpdateCheckStatus.updateAvailable : UpdateCheckStatus.upToDate,
          releaseInfo: _latestRelease,
        );
      } else if (response.statusCode == 404) {
        // No hay releases publicadas
        return UpdateResult(
          status: UpdateCheckStatus.upToDate,
          message: 'No hay releases publicadas aún',
        );
      } else {
        return UpdateResult(
          status: UpdateCheckStatus.error,
          message: 'Error del servidor: ${response.statusCode}',
        );
      }
    } on SocketException {
      return UpdateResult(
        status: UpdateCheckStatus.error,
        message: 'Sin conexión a internet',
      );
    } catch (e) {
      debugPrint('GitHubUpdateService: Error verificando actualizaciones: $e');
      return UpdateResult(
        status: UpdateCheckStatus.error,
        message: 'Error al verificar: $e',
      );
    }
  }
  
  /// Compara dos versiones semánticas
  /// Retorna: -1 si v1 < v2, 0 si v1 == v2, 1 si v1 > v2
  static int _compareVersions(String v1, String v2) {
    final parts1 = v1.split('.').map((e) => int.tryParse(e) ?? 0).toList();
    final parts2 = v2.split('.').map((e) => int.tryParse(e) ?? 0).toList();
    
    // Asegurar que ambas listas tengan la misma longitud
    while (parts1.length < 3) {
      parts1.add(0);
    }
    while (parts2.length < 3) {
      parts2.add(0);
    }
    
    for (int i = 0; i < 3; i++) {
      if (parts1[i] < parts2[i]) return -1;
      if (parts1[i] > parts2[i]) return 1;
    }
    
    return 0;
  }
  
  /// Descarga el APK de la última release
  /// 
  /// [onProgress] - Callback para reportar progreso (0.0 - 1.0)
  /// Retorna la ruta del archivo descargado o null si falla
  static Future<String?> downloadApk({
    void Function(double progress)? onProgress,
    CancelToken? cancelToken,
  }) async {
    if (_latestRelease?.apkDownloadUrl == null) {
      debugPrint('GitHubUpdateService: No hay URL de descarga disponible');
      return null;
    }
    
    try {
      // Obtener directorio de descargas
      final directory = await getExternalStorageDirectory();
      if (directory == null) {
        debugPrint('GitHubUpdateService: No se pudo obtener directorio de almacenamiento');
        return null;
      }
      
      final fileName = 'netflow-${_latestRelease!.version}.apk';
      final filePath = '${directory.path}/$fileName';
      
      // Descargar con Dio para tener progreso
      final dio = Dio();
      
      await dio.download(
        _latestRelease!.apkDownloadUrl!,
        filePath,
        cancelToken: cancelToken,
        onReceiveProgress: (received, total) {
          if (total > 0) {
            onProgress?.call(received / total);
          }
        },
        options: Options(
          headers: {
            'User-Agent': 'NetFlow-App',
          },
        ),
      );
      
      debugPrint('GitHubUpdateService: APK descargado en $filePath');
      return filePath;
      
    } on DioException catch (e) {
      if (e.type == DioExceptionType.cancel) {
        debugPrint('GitHubUpdateService: Descarga cancelada');
      } else {
        debugPrint('GitHubUpdateService: Error en descarga: $e');
      }
      return null;
    } catch (e) {
      debugPrint('GitHubUpdateService: Error descargando APK: $e');
      return null;
    }
  }
  
  /// Abre la página de releases en el navegador
  static Future<bool> openReleasesPage() async {
    final url = Uri.parse('https://github.com/$_owner/$_repo/releases');
    if (await canLaunchUrl(url)) {
      return await launchUrl(url, mode: LaunchMode.externalApplication);
    }
    return false;
  }
  
  /// Abre el enlace de descarga directa en el navegador
  static Future<bool> openDownloadInBrowser() async {
    if (_latestRelease?.apkDownloadUrl == null) return false;
    
    final url = Uri.parse(_latestRelease!.apkDownloadUrl!);
    if (await canLaunchUrl(url)) {
      return await launchUrl(url, mode: LaunchMode.externalApplication);
    }
    return false;
  }
}

/// Estados de verificación de actualizaciones
enum UpdateCheckStatus {
  upToDate,
  updateAvailable,
  error,
}

/// Resultado de la verificación de actualizaciones
class UpdateResult {
  final UpdateCheckStatus status;
  final ReleaseInfo? releaseInfo;
  final String? message;
  
  const UpdateResult({
    required this.status,
    this.releaseInfo,
    this.message,
  });
}

/// Información de una release de GitHub
class ReleaseInfo {
  final String version;
  final String tagName;
  final String releaseNotes;
  final String htmlUrl;
  final String? apkDownloadUrl;
  final int? apkSize;
  final DateTime? publishedAt;
  
  const ReleaseInfo({
    required this.version,
    required this.tagName,
    required this.releaseNotes,
    required this.htmlUrl,
    this.apkDownloadUrl,
    this.apkSize,
    this.publishedAt,
  });
  
  /// Tamaño del APK formateado
  String get apkSizeFormatted {
    if (apkSize == null) return '';
    final mb = apkSize! / (1024 * 1024);
    return '${mb.toStringAsFixed(1)} MB';
  }
  
  /// Fecha de publicación formateada
  String get publishedAtFormatted {
    if (publishedAt == null) return '';
    return '${publishedAt!.day}/${publishedAt!.month}/${publishedAt!.year}';
  }
}
