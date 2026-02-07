# NetFlow

Monitor de uso de datos moviles y WiFi en tiempo real para Android, construido con Flutter.

## Caracteristicas

- Monitoreo en tiempo real de velocidad de bajada y subida (bytes/s o bits/s)
- Notificacion persistente con icono dinamico de velocidad en barra de estado
- Diferenciacion WiFi / Datos moviles con deteccion automatica
- Historial diario de consumo con graficos
- Servicio en background con auto-inicio al boot y optimizacion de bateria
- Limite de datos configurable con ciclo de facturacion y alerta
- Actualizaciones via GitHub Releases
- Material Design 3 con Dynamic Color (Android 12+)
- Icono monocromatico tematico (Android 13+)

## Requisitos

- **Minimo:** Android 8.0 (API 26) -- funcionalidad completa
- **Recomendado:** Android 12+ (API 31) -- Dynamic Color / Material You
- **Target SDK:** Android 15 (API 35)

## Permisos

**Solicitados al usuario:**

| Permiso | Motivo |
|---------|--------|
| `READ_PHONE_STATE` | Monitorear uso de datos moviles |
| `POST_NOTIFICATIONS` (Android 13+) | Notificacion de velocidad y alerta de limite |
| `ACCESS_FINE_LOCATION` *(opcional)* | Obtener SSID de la red WiFi |
| `IGNORE_BATTERY_OPTIMIZATIONS` *(opcional)* | Evitar que el sistema detenga el servicio |

**Automaticos:**

`INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `PACKAGE_USAGE_STATS`

## Arquitectura

El monitoreo corre en un **TaskHandler** (background isolate) cada segundo, independiente del UI.

**Plugin local `netflow_traffic_stats`:** expone `android.net.TrafficStats`, genera iconos dinamicos de velocidad y gestiona notificaciones nativas.

## Build

```bash
flutter pub get
flutter build apk --release
```

Firma de release en `android/key.properties` (excluido de git).

## Instalacion

1. Descargar APK desde [GitHub Releases](https://github.com/dony-aep/netflow/releases)
2. Instalar en el dispositivo
3. Conceder permisos al abrir la app

## Licencia

MIT. Ver [LICENSE](LICENSE).
