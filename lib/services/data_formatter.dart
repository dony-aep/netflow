/// Utilidades para formatear tamaños de datos
class DataFormatter {
  DataFormatter._();
  
  /// Unidades de medida de datos (bytes)
  static const List<String> _bytesUnits = ['B', 'KB', 'MB', 'GB', 'TB'];
  
  /// Unidades de medida de datos (bits)
  static const List<String> _bitsUnits = ['b', 'Kb', 'Mb', 'Gb', 'Tb'];
  
  /// Formatea bytes a una representación legible (KB, MB, GB, etc.)
  /// 
  /// [bytes] - Cantidad de bytes a formatear
  /// [decimals] - Número de decimales a mostrar (por defecto 2)
  static String formatBytes(int bytes, {int decimals = 2}) {
    if (bytes <= 0) return '0 B';
    
    int unitIndex = 0;
    double size = bytes.toDouble();
    
    while (size >= 1024 && unitIndex < _bytesUnits.length - 1) {
      size /= 1024;
      unitIndex++;
    }
    
    // Si es menor a 10, mostrar más decimales para precisión
    if (size < 10 && unitIndex > 0) {
      return '${size.toStringAsFixed(decimals)} ${_bytesUnits[unitIndex]}';
    }
    
    // Para valores más grandes, menos decimales
    return '${size.toStringAsFixed(size < 100 ? 1 : 0)} ${_bytesUnits[unitIndex]}';
  }
  
  /// Formatea bits a una representación legible (Kb, Mb, Gb, etc.)
  /// 
  /// [bits] - Cantidad de bits a formatear
  /// [decimals] - Número de decimales a mostrar (por defecto 2)
  static String formatBits(int bits, {int decimals = 2}) {
    if (bits <= 0) return '0 b';
    
    int unitIndex = 0;
    double size = bits.toDouble();
    
    // Para bits usamos 1000 (sistema SI) en lugar de 1024
    while (size >= 1000 && unitIndex < _bitsUnits.length - 1) {
      size /= 1000;
      unitIndex++;
    }
    
    if (size < 10 && unitIndex > 0) {
      return '${size.toStringAsFixed(decimals)} ${_bitsUnits[unitIndex]}';
    }
    
    return '${size.toStringAsFixed(size < 100 ? 1 : 0)} ${_bitsUnits[unitIndex]}';
  }
  
  /// Formatea velocidad de datos (bytes por segundo)
  /// 
  /// [bytesPerSecond] - Velocidad en bytes por segundo
  static String formatSpeed(int bytesPerSecond) {
    if (bytesPerSecond <= 0) return '0 B/s';
    return '${formatBytes(bytesPerSecond)}/s';
  }
  
  /// Formatea velocidad de datos en bits por segundo
  /// 
  /// [bytesPerSecond] - Velocidad en bytes por segundo (se convierte a bits)
  static String formatSpeedBits(int bytesPerSecond) {
    if (bytesPerSecond <= 0) return '0 bps';
    // Convertir bytes a bits (1 byte = 8 bits)
    final bitsPerSecond = bytesPerSecond * 8;
    return '${formatBits(bitsPerSecond)}ps';
  }
  
  /// Formatea velocidad según la unidad preferida
  /// 
  /// [bytesPerSecond] - Velocidad en bytes por segundo
  /// [useBits] - true para bits/s, false para bytes/s
  static String formatSpeedWithUnit(int bytesPerSecond, {bool useBits = false}) {
    if (useBits) {
      return formatSpeedBits(bytesPerSecond);
    }
    return formatSpeed(bytesPerSecond);
  }
  
  /// Formatea velocidad con etiquetas descriptivas
  /// 
  /// [download] - Velocidad de bajada en bytes/s
  /// [upload] - Velocidad de subida en bytes/s
  /// [useBits] - true para bits/s, false para bytes/s
  static String formatSpeedWithLabels({
    required int download,
    required int upload,
    bool useBits = false,
  }) {
    final downloadText = formatSpeedWithUnit(download, useBits: useBits);
    final uploadText = formatSpeedWithUnit(upload, useBits: useBits);
    return 'Bajada: $downloadText  Subida: $uploadText';
  }
  
  /// Obtiene solo el valor numérico formateado
  static String formatBytesValue(int bytes, {int decimals = 2}) {
    if (bytes <= 0) return '0';
    
    int unitIndex = 0;
    double size = bytes.toDouble();
    
    while (size >= 1024 && unitIndex < _bytesUnits.length - 1) {
      size /= 1024;
      unitIndex++;
    }
    
    if (size < 10 && unitIndex > 0) {
      return size.toStringAsFixed(decimals);
    }
    
    return size.toStringAsFixed(size < 100 ? 1 : 0);
  }
  
  /// Obtiene la unidad de medida para una cantidad de bytes
  static String getUnit(int bytes) {
    if (bytes <= 0) return 'B';
    
    int unitIndex = 0;
    double size = bytes.toDouble();
    
    while (size >= 1024 && unitIndex < _bytesUnits.length - 1) {
      size /= 1024;
      unitIndex++;
    }
    
    return _bytesUnits[unitIndex];
  }
}
