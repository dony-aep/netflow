# Changelog

Todos los cambios relevantes de este proyecto se documentaran en este archivo.

El formato esta basado en [Keep a Changelog],
y este proyecto sigue [Semantic Versioning].

## [Unreleased]

### Cambiado

- Sin cambios por ahora.

## [1.0.1] - 2026-03-26

### Corregido

- Se mejoro la lectura del SSID en segundo plano para evitar que desaparezca cuando Android devuelve valores temporales desconocidos.
- Se agrego fallback al ultimo SSID valido en cache cuando no se puede leer el nombre de red en background.
- Se valido el estado del servicio de ubicacion del sistema antes de consultar SSID en background.

### Cambiado

- Se actualizo el flujo de permisos de ubicacion para solicitar `locationWhenInUse` y luego intentar `locationAlways`.
- Se agregaron helpers de permisos para verificar acceso de ubicacion en segundo plano y estado del servicio de ubicacion.
- Se mejoro la pantalla de opciones avanzadas para mostrar el estado completo requerido para SSID estable en segundo plano.

### Seguridad

- Se agrego el permiso `ACCESS_BACKGROUND_LOCATION` (Android 10+) en el manifiesto para soportar lectura de SSID durante monitoreo en segundo plano.

### Documentacion

- Se actualizo README con el nuevo permiso opcional `ACCESS_BACKGROUND_LOCATION` y su motivo.

## [1.0.0] - 2026-02-07

### Anadido

- Primera version de NetFlow para Android.
- Monitoreo en tiempo real del trafico de red para velocidades de bajada/subida en bytes/s y bits/s.
- Notificacion persistente en primer plano con icono dinamico de velocidad en la barra de estado.
- Deteccion automatica del tipo de red (WiFi vs datos moviles).
- Historial diario de uso de datos con visualizacion en graficos.
- Limite de datos configurable con ciclo de facturacion y notificacion de alerta.
- Servicio de monitoreo en segundo plano basado en TaskHandler en un isolate separado.
- Flujo de actualizacion desde la app mediante GitHub Releases con descarga directa de APK.
- Interfaz con Material Design 3 y soporte para Dynamic Color, incluyendo icono monocromatico.
- Plugin local `netflow_traffic_stats` para acceso nativo a TrafficStats, generacion de iconos de velocidad y utilidades de notificaciones.
- Estructura Flutter multiplataforma (Android, iOS, Linux, macOS, Web y Windows).

### Cambiado

- Se actualizo el namespace y el applicationId de Android desde `com.netflow.netflow` a `com.donyaep.netflow`.
- Se movio el paquete de `MainActivity` a `com.donyaep.netflow`.
- Se configuro la firma de compilacion release para cargar valores del keystore desde `android/key.properties`.
- Se agrego fallback para firmar con debug cuando no existe `android/key.properties`.
- Se habilito minificacion R8/ProGuard y reduccion de recursos para builds release.
- Se agregaron reglas dedicadas de ProGuard en `android/app/proguard-rules.pro`.
- Se amplio el README con requisitos, permisos, notas de arquitectura, nota de firma release y pasos de instalacion.

### Seguridad

- Se actualizo `.gitignore` para excluir archivos de firma: `android/key.properties`, `android/app/*.jks` y `android/app/*.keystore`.

### Notas

- Esta version consolida todo el trabajo introducido en los primeros tres commits del repositorio:
  - `f1c20f1` Initial commit: NetFlow - Network data monitoring app
  - `66ab173` chore: configurar release signing, ProGuard y applicationId
  - `a347c9f` docs: actualizar README con requisitos y permisos

[Keep a Changelog]: https://keepachangelog.com/en/1.1.0/
[Semantic Versioning]: https://semver.org/spec/v2.0.0.html
[Unreleased]: https://github.com/dony-aep/netflow/compare/1.0.1...HEAD
[1.0.1]: https://github.com/dony-aep/netflow/compare/a347c9f1d9b27467675f0314ffbb2366db7bd9ca...HEAD
[1.0.0]: https://github.com/dony-aep/netflow/commits/a347c9f1d9b27467675f0314ffbb2366db7bd9ca