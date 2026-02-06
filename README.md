# NetFlow

Monitor de uso de datos móviles y WiFi en tiempo real para Android, construido con Flutter.

## Características

- **Monitoreo en tiempo real** de velocidad de bajada y subida (bytes/s o bits/s)
- **Notificación persistente** con icono dinámico que muestra la velocidad actual en la barra de estado
- **Diferenciación WiFi / Datos móviles** con detección automática del tipo de red
- **Historial diario** de consumo de datos con gráficos
- **Servicio en background robusto** que sobrevive al cierre de la app (auto-inicio al boot, optimización de batería)
- **Límite de datos configurable** con ciclo de facturación personalizable y notificación al alcanzar el límite
- **Actualizaciones vía GitHub** con descarga directa de APK desde releases
- **Material Design 3 Expressive** con tema dinámico (Dynamic Color)
- **Icono monocromático** adaptado para Dynamic Color y barra de estado

## Arquitectura

El polling de red vive en un **TaskHandler** (background isolate) que se ejecuta cada segundo, independiente del UI. Esto garantiza monitoreo continuo incluso cuando la app está cerrada.

### Plugin local: `netflow_traffic_stats`

Plugin Flutter interno que expone `android.net.TrafficStats` desde cualquier isolate, e incluye:
- Generación de iconos dinámicos de velocidad para la notificación
- Notificación de alerta al alcanzar el límite de datos configurado

## Tecnologías

- **Flutter SDK** ^3.10.7
- **flutter_foreground_task** ^9.2.0 — Servicio foreground persistente
- **connectivity_plus** / **network_info_plus** — Detección de red y SSID WiFi
- **sqflite** — Base de datos local para historial
- **fl_chart** — Gráficos de consumo
- **shared_preferences** — Persistencia de configuración
- **Material Design 3** — Diseño moderno con tema Expressive y Dynamic Color

## Requisitos

- Android API 24+ (Android 7.0)
- Permiso de ubicación (para obtener SSID WiFi)
- Permiso de notificaciones (Android 13+)

## Build

```bash
flutter pub get
flutter build apk --release
```

## Licencia

Este proyecto está bajo la licencia MIT. Ver [LICENSE](LICENSE) para más detalles.
