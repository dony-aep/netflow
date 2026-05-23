# Changelog

Todos los cambios relevantes de este proyecto se documentaran en este archivo.

El formato esta basado en [Keep a Changelog],
y este proyecto sigue [Semantic Versioning].

## [2.0.4] - 2026-05-23

### Corregido

- Calculo incorrecto de consumo WiFi y datos moviles: se eliminó la derivacion de bytes WiFi por sustraccion (`total - mobile`) que causaba valores inflados y duplicacion al cambiar de red. Ahora cada delta de bytes se atribuye exclusivamente a la red activa en el momento del muestreo.
- Alerta de limite de datos ahora cuenta solo bytes de red movil en lugar de sumar WiFi + Mobile, reflejando correctamente el consumo del plan de datos celular.

## [2.0.3] - 2026-05-07

### Cambiado

- Boton de descarga de actualizacion ahora redirige a la pagina de la ultima release en GitHub en lugar de intentar la descarga directa del APK, evitando que Chrome bloquee o no complete la descarga.
- Texto informativo actualizado para reflejar el nuevo flujo de descarga.

## [2.0.2] - 2026-05-07

### Corregido

- Reinicio de contadores diarios en la notificacion al cambiar de dia: los acumuladores en memoria de WiFi y datos moviles no se reiniciaban a medianoche, mostrando el consumo del dia anterior. Se agrego deteccion de cambio de fecha en el loop de polling que recarga los contadores desde la base de datos.

## [2.0.1] - 2026-05-04

### Corregido

- Lectura de SSID en la notificacion: cuando la API moderna (`NetworkCapabilities.transportInfo`) devuelve `<unknown ssid>`, ahora se usa el fallback deprecated (`WifiManager.connectionInfo`) que en muchos dispositivos retorna el nombre real de la red.
- Restauracion de contadores diarios al reiniciar el monitoreo: al detener e iniciar nuevamente el servicio, los acumuladores de WiFi y datos moviles se cargan desde la base de datos en lugar de empezar desde cero.

## [2.0.0] - 2026-05-03

### Anadido

- Migracion completa de Flutter a Kotlin nativo con Jetpack Compose.
- Interfaz Material 3 Expressive con Dynamic Color, MotionScheme y tipografia Google Sans.
- Navegacion con Navigation Compose (Home, History, Settings, Advanced, Updates, About).
- Persistencia local con Room Database para historial diario de consumo.
- DataStore Preferences para configuracion de usuario.
- Servicio foreground nativo con coroutines para monitoreo en segundo plano.
- Notificacion persistente con icono dinamico de velocidad generado en tiempo real.
- Alerta de limite de datos mensual basada en ciclo de facturacion completo desde la base de datos.
- BroadcastReceiver para inicio automatico tras reinicio del dispositivo.
- Pantalla de historial con calendario, filtros por periodo y resumen comparativo.
- Pantalla de actualizaciones con verificacion desde GitHub Releases API.
- Pantalla de opciones avanzadas para permisos de ubicacion en background y bateria.
- Configuracion de release signing con keystore, minificacion R8 y shrink resources.
- Reglas ProGuard para Room, DataStore enums, Coroutines y Compose.
- Exportacion de esquema Room para soporte de migraciones futuras.

### Cambiado

- Arquitectura completamente reescrita: de Flutter (Dart + isolates) a Kotlin (Coroutines + StateFlow + ViewModel).
- Plugin local `netflow_traffic_stats` reemplazado por acceso directo a `TrafficStats` API.
- Motor de UI de Flutter/Widgets reemplazado por Jetpack Compose con Material 3 Expressive.
- Monitoreo basado en `TaskHandler` (isolate) reemplazado por `Service` foreground con `CoroutineScope`.
- Persistencia migrada de SQLite directo a Room con DAO tipado.
- Configuracion migrada de SharedPreferences a DataStore Preferences con Flow reactivo.
- Target SDK actualizado a API 36 con Compile SDK 36.1.

### Corregido

- Race condition en `MonitoringStateStore` corregida con `MutableStateFlow.update{}` atomico.
- DataStore singleton garantizado: `BootCompletedReceiver` usa `AppContainer` en lugar de crear instancias duplicadas.
- Alerta de limite de datos ahora calcula el total del ciclo de facturacion completo desde la DB, no solo bytes del dia en memoria.
- Eliminadas APIs deprecated (`Window.statusBarColor`, `Window.navigationBarColor`).

### Seguridad

- Keystore y `key.properties` excluidos del repositorio via `.gitignore`.
- R8 habilitado en release para ofuscacion y reduccion de codigo.

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
[2.0.4]: https://github.com/dony-aep/netflow/compare/2.0.3...2.0.4
[2.0.3]: https://github.com/dony-aep/netflow/compare/2.0.2...2.0.3
[2.0.2]: https://github.com/dony-aep/netflow/compare/2.0.1...2.0.2
[2.0.1]: https://github.com/dony-aep/netflow/compare/2.0.0...2.0.1
[2.0.0]: https://github.com/dony-aep/netflow/compare/1.0.1...2.0.0
[1.0.1]: https://github.com/dony-aep/netflow/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/dony-aep/netflow/commits/1.0.0
