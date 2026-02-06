import 'network_type.dart';

/// Modelo para el registro diario de uso de datos
class DailyUsage {
  /// ID único del registro
  final int? id;
  
  /// Fecha del registro (solo fecha, sin hora)
  final DateTime date;
  
  /// Bytes de bajada en WiFi
  final int wifiReceived;
  
  /// Bytes de subida en WiFi
  final int wifiSent;
  
  /// Bytes de bajada en datos móviles
  final int mobileReceived;
  
  /// Bytes de subida en datos móviles
  final int mobileSent;

  const DailyUsage({
    this.id,
    required this.date,
    this.wifiReceived = 0,
    this.wifiSent = 0,
    this.mobileReceived = 0,
    this.mobileSent = 0,
  });

  /// Total de bytes en WiFi
  int get totalWifi => wifiReceived + wifiSent;
  
  /// Total de bytes en datos móviles
  int get totalMobile => mobileReceived + mobileSent;
  
  /// Total general de bytes
  int get totalBytes => totalWifi + totalMobile;
  
  /// Total de bajada (WiFi + móvil)
  int get totalReceived => wifiReceived + mobileReceived;
  
  /// Total de subida (WiFi + móvil)
  int get totalSent => wifiSent + mobileSent;

  /// Crea una copia con valores actualizados
  DailyUsage copyWith({
    int? id,
    DateTime? date,
    int? wifiReceived,
    int? wifiSent,
    int? mobileReceived,
    int? mobileSent,
  }) {
    return DailyUsage(
      id: id ?? this.id,
      date: date ?? this.date,
      wifiReceived: wifiReceived ?? this.wifiReceived,
      wifiSent: wifiSent ?? this.wifiSent,
      mobileReceived: mobileReceived ?? this.mobileReceived,
      mobileSent: mobileSent ?? this.mobileSent,
    );
  }

  /// Añade uso de datos según el tipo de red
  DailyUsage addUsage({
    required int received,
    required int sent,
    required NetworkType networkType,
  }) {
    if (networkType == NetworkType.wifi) {
      return copyWith(
        wifiReceived: wifiReceived + received,
        wifiSent: wifiSent + sent,
      );
    } else if (networkType == NetworkType.mobile) {
      return copyWith(
        mobileReceived: mobileReceived + received,
        mobileSent: mobileSent + sent,
      );
    }
    return this;
  }

  /// Convierte a Map para base de datos
  Map<String, dynamic> toMap() {
    return {
      if (id != null) 'id': id,
      'date': _dateToString(date),
      'wifiReceived': wifiReceived,
      'wifiSent': wifiSent,
      'mobileReceived': mobileReceived,
      'mobileSent': mobileSent,
    };
  }

  /// Crea desde Map de base de datos
  factory DailyUsage.fromMap(Map<String, dynamic> map) {
    return DailyUsage(
      id: map['id'] as int?,
      date: _stringToDate(map['date'] as String),
      wifiReceived: map['wifiReceived'] as int? ?? 0,
      wifiSent: map['wifiSent'] as int? ?? 0,
      mobileReceived: map['mobileReceived'] as int? ?? 0,
      mobileSent: map['mobileSent'] as int? ?? 0,
    );
  }

  /// Crea un registro vacío para hoy
  factory DailyUsage.today() {
    final now = DateTime.now();
    return DailyUsage(
      date: DateTime(now.year, now.month, now.day),
    );
  }

  /// Convierte fecha a string para almacenamiento
  static String _dateToString(DateTime date) {
    return '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
  }

  /// Convierte string a fecha
  static DateTime _stringToDate(String dateString) {
    final parts = dateString.split('-');
    return DateTime(
      int.parse(parts[0]),
      int.parse(parts[1]),
      int.parse(parts[2]),
    );
  }

  @override
  String toString() {
    return 'DailyUsage(date: $date, wifi: $totalWifi, mobile: $totalMobile)';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is DailyUsage &&
        other.date.year == date.year &&
        other.date.month == date.month &&
        other.date.day == date.day;
  }

  @override
  int get hashCode => date.hashCode;
}
