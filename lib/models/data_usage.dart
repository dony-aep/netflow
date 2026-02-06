import 'network_type.dart';

/// Modelo para representar el uso de datos en un momento específico
class DataUsage {
  /// Bytes descargados (bajada)
  final int bytesReceived;
  
  /// Bytes enviados (subida)
  final int bytesSent;
  
  /// Tipo de red activa
  final NetworkType networkType;
  
  /// Timestamp de la medición
  final DateTime timestamp;

  const DataUsage({
    required this.bytesReceived,
    required this.bytesSent,
    required this.networkType,
    required this.timestamp,
  });

  /// Total de bytes transferidos
  int get totalBytes => bytesReceived + bytesSent;

  /// Crea una copia con valores actualizados
  DataUsage copyWith({
    int? bytesReceived,
    int? bytesSent,
    NetworkType? networkType,
    DateTime? timestamp,
  }) {
    return DataUsage(
      bytesReceived: bytesReceived ?? this.bytesReceived,
      bytesSent: bytesSent ?? this.bytesSent,
      networkType: networkType ?? this.networkType,
      timestamp: timestamp ?? this.timestamp,
    );
  }

  /// Convierte a Map para almacenamiento
  Map<String, dynamic> toMap() {
    return {
      'bytesReceived': bytesReceived,
      'bytesSent': bytesSent,
      'networkType': networkType.index,
      'timestamp': timestamp.millisecondsSinceEpoch,
    };
  }

  /// Crea desde Map
  factory DataUsage.fromMap(Map<String, dynamic> map) {
    return DataUsage(
      bytesReceived: map['bytesReceived'] as int,
      bytesSent: map['bytesSent'] as int,
      networkType: NetworkType.values[map['networkType'] as int],
      timestamp: DateTime.fromMillisecondsSinceEpoch(map['timestamp'] as int),
    );
  }

  /// Uso de datos vacío
  factory DataUsage.empty() {
    return DataUsage(
      bytesReceived: 0,
      bytesSent: 0,
      networkType: NetworkType.none,
      timestamp: DateTime.now(),
    );
  }

  @override
  String toString() {
    return 'DataUsage(received: $bytesReceived, sent: $bytesSent, type: $networkType)';
  }
}
