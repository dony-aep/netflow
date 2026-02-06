/// Tipos de conexión de red disponibles
enum NetworkType {
  /// Conexión WiFi
  wifi,
  
  /// Datos móviles (celular)
  mobile,
  
  /// Sin conexión
  none,
}

/// Extensión para obtener propiedades útiles del tipo de red
extension NetworkTypeExtension on NetworkType {
  /// Nombre legible del tipo de red
  String get displayName {
    switch (this) {
      case NetworkType.wifi:
        return 'WiFi';
      case NetworkType.mobile:
        return 'Datos móviles';
      case NetworkType.none:
        return 'Sin conexión';
    }
  }
  
  /// Icono asociado al tipo de red
  String get iconName {
    switch (this) {
      case NetworkType.wifi:
        return 'wifi';
      case NetworkType.mobile:
        return 'signal_cellular_alt';
      case NetworkType.none:
        return 'signal_cellular_off';
    }
  }
  
  /// Indica si hay conexión activa
  bool get isConnected => this != NetworkType.none;
}
