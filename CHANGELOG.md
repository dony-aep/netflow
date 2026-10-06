# Changelog

Todos los cambios relevantes de este proyecto se documentarán en este archivo.

El formato está basado en [Keep a Changelog],
y este proyecto sigue [Semantic Versioning].

## [2.1.0] - 2026-10-06

### Añadido

- El historial indica cuál fue el día de mayor consumo del mes y cuánto se gastó.

### Cambiado

- Diseño nuevo en todas las pantallas. La cifra principal de cada una va dentro de una forma; en el inicio esa forma se ondula y gira más rápido cuanto más tráfico hay, así que la actividad de la red se nota sin leer el número.
- El consumo de hoy se lee en una línea, «Hoy llevas», con una barra que lo reparte entre WiFi y datos móviles. Sustituye a las cinco tarjetas anteriores.
- En el calendario del historial cada día se dibuja más intenso cuanto más se consumió, para comparar los días sin abrirlos.
- El detalle de un día muestra el total, la bajada, la subida y el reparto entre WiFi y datos móviles. Ya no desglosa bajada y subida por cada red.
- El paso de una pantalla a otra es un deslizamiento, sin el oscurecimiento que producía el fundido anterior.
- «Ajustes avanzados» pasa a llamarse «Permisos y batería». En Ajustes, «Ocultar en lockscreen» es ahora «Ocultar en la pantalla de bloqueo» y «Habilitar límite» es «Avisarme al llegar a un límite».
- La notificación solo se actualiza cuando cambia su contenido y la pantalla está encendida. Con la pantalla apagada el icono de velocidad conserva su último valor; el conteo de datos no se ve afectado.
- Menos trabajo en segundo plano: el icono de velocidad se dibuja a su tamaño real y, con el límite activo, el consumo del ciclo ya no se consulta en la base de datos en cada muestra.

### Eliminado

- Permisos `READ_PHONE_STATE` y `WAKE_LOCK`, que la app declaraba sin usar.

### Corregido

- El servicio de monitoreo se declara como `specialUse`. Con el tipo anterior, `dataSync`, Android 15 o superior lo limita a seis horas diarias en segundo plano y no permite arrancarlo al encender el teléfono.
- Error al leer el nombre de la red WiFi en Android 8 y 9 con el permiso de ubicación concedido: se llamaba a una función que solo existe desde Android 10.
- La barra de límite del inicio compara el límite con los datos móviles del ciclo. Antes usaba todo el consumo de hoy, WiFi incluido, y podía marcar un 80 % sin haber gastado datos móviles.
- Reiniciar los contadores del día mientras se tomaba una muestra podía dejar cifras incoherentes.

## [2.0.4] - 2026-05-23

### Corregido

- Cálculo incorrecto de consumo WiFi y datos móviles: se eliminó la derivación de bytes WiFi por sustracción (`total - mobile`) que causaba valores inflados y duplicación al cambiar de red. Ahora cada delta de bytes se atribuye exclusivamente a la red activa en el momento del muestreo.
- Alerta de límite de datos ahora cuenta solo bytes de red móvil en lugar de sumar WiFi + Mobile, reflejando correctamente el consumo del plan de datos celular.

## [2.0.3] - 2026-05-07

### Cambiado

- Botón de descarga de actualización ahora redirige a la página de la última release en GitHub en lugar de intentar la descarga directa del APK, evitando que Chrome bloquee o no complete la descarga.
- Texto informativo actualizado para reflejar el nuevo flujo de descarga.

## [2.0.2] - 2026-05-07

### Corregido

- Reinicio de contadores diarios en la notificación al cambiar de día: los acumuladores en memoria de WiFi y datos móviles no se reiniciaban a medianoche, mostrando el consumo del día anterior. Se agregó detección de cambio de fecha en el loop de polling que recarga los contadores desde la base de datos.

## [2.0.1] - 2026-05-04

### Corregido

- Lectura de SSID en la notificación: cuando la API moderna (`NetworkCapabilities.transportInfo`) devuelve `<unknown ssid>`, ahora se usa el fallback deprecated (`WifiManager.connectionInfo`) que en muchos dispositivos retorna el nombre real de la red.
- Restauración de contadores diarios al reiniciar el monitoreo: al detener e iniciar nuevamente el servicio, los acumuladores de WiFi y datos móviles se cargan desde la base de datos en lugar de empezar desde cero.

## [2.0.0] - 2026-05-03

### Añadido

- Migración completa de Flutter a Kotlin nativo con Jetpack Compose.
- Interfaz Material 3 Expressive con Dynamic Color, MotionScheme y tipografía Google Sans.
- Navegación con Navigation Compose (Home, History, Settings, Advanced, Updates, About).
- Persistencia local con Room Database para historial diario de consumo.
- DataStore Preferences para configuración de usuario.
- Servicio foreground nativo con coroutines para monitoreo en segundo plano.
- Notificación persistente con icono dinámico de velocidad generado en tiempo real.
- Alerta de límite de datos mensual basada en ciclo de facturación completo desde la base de datos.
- BroadcastReceiver para inicio automático tras reinicio del dispositivo.
- Pantalla de historial con calendario, filtros por periodo y resumen comparativo.
- Pantalla de actualizaciones con verificación desde GitHub Releases API.
- Pantalla de opciones avanzadas para permisos de ubicación en background y batería.
- Configuración de release signing con keystore, minificación R8 y shrink resources.
- Reglas ProGuard para Room, DataStore enums, Coroutines y Compose.
- Exportación de esquema Room para soporte de migraciones futuras.

### Cambiado

- Arquitectura completamente reescrita: de Flutter (Dart + isolates) a Kotlin (Coroutines + StateFlow + ViewModel).
- Plugin local `netflow_traffic_stats` reemplazado por acceso directo a `TrafficStats` API.
- Motor de UI de Flutter/Widgets reemplazado por Jetpack Compose con Material 3 Expressive.
- Monitoreo basado en `TaskHandler` (isolate) reemplazado por `Service` foreground con `CoroutineScope`.
- Persistencia migrada de SQLite directo a Room con DAO tipado.
- Configuración migrada de SharedPreferences a DataStore Preferences con Flow reactivo.
- Target SDK actualizado a API 36 con Compile SDK 36.1.

### Corregido

- Race condition en `MonitoringStateStore` corregida con `MutableStateFlow.update{}` atómico.
- DataStore singleton garantizado: `BootCompletedReceiver` usa `AppContainer` en lugar de crear instancias duplicadas.
- Alerta de límite de datos ahora calcula el total del ciclo de facturación completo desde la DB, no solo bytes del día en memoria.
- Eliminadas APIs deprecated (`Window.statusBarColor`, `Window.navigationBarColor`).

### Seguridad

- Keystore y `key.properties` excluidos del repositorio vía `.gitignore`.
- R8 habilitado en release para ofuscación y reducción de código.

## [1.0.1] - 2026-03-26

### Corregido

- Se mejoró la lectura del SSID en segundo plano para evitar que desaparezca cuando Android devuelve valores temporales desconocidos.
- Se agregó fallback al último SSID válido en caché cuando no se puede leer el nombre de red en background.
- Se validó el estado del servicio de ubicación del sistema antes de consultar SSID en background.

### Cambiado

- Se actualizó el flujo de permisos de ubicación para solicitar `locationWhenInUse` y luego intentar `locationAlways`.
- Se agregaron helpers de permisos para verificar acceso de ubicación en segundo plano y estado del servicio de ubicación.
- Se mejoró la pantalla de opciones avanzadas para mostrar el estado completo requerido para SSID estable en segundo plano.

### Seguridad

- Se agregó el permiso `ACCESS_BACKGROUND_LOCATION` (Android 10+) en el manifiesto para soportar lectura de SSID durante monitoreo en segundo plano.

### Documentación

- Se actualizó README con el nuevo permiso opcional `ACCESS_BACKGROUND_LOCATION` y su motivo.

## [1.0.0] - 2026-02-07

### Añadido

- Primera versión de NetFlow para Android.
- Monitoreo en tiempo real del tráfico de red para velocidades de bajada/subida en bytes/s y bits/s.
- Notificación persistente en primer plano con icono dinámico de velocidad en la barra de estado.
- Detección automática del tipo de red (WiFi vs datos móviles).
- Historial diario de uso de datos con visualización en gráficos.
- Límite de datos configurable con ciclo de facturación y notificación de alerta.
- Servicio de monitoreo en segundo plano basado en TaskHandler en un isolate separado.
- Flujo de actualización desde la app mediante GitHub Releases con descarga directa de APK.
- Interfaz con Material Design 3 y soporte para Dynamic Color, incluyendo icono monocromático.
- Plugin local `netflow_traffic_stats` para acceso nativo a TrafficStats, generación de iconos de velocidad y utilidades de notificaciones.

### Cambiado

- Se actualizó el namespace y el applicationId de Android desde `com.netflow.netflow` a `com.donyaep.netflow`.
- Se movió el paquete de `MainActivity` a `com.donyaep.netflow`.
- Se configuró la firma de compilación release para cargar valores del keystore desde `android/key.properties`.
- Se agregó fallback para firmar con debug cuando no existe `android/key.properties`.
- Se habilitó minificación R8/ProGuard y reducción de recursos para builds release.
- Se agregaron reglas dedicadas de ProGuard en `android/app/proguard-rules.pro`.
- Se amplió el README con requisitos, permisos, notas de arquitectura, nota de firma release y pasos de instalación.

### Seguridad

- Se actualizó `.gitignore` para excluir archivos de firma: `android/key.properties`, `android/app/*.jks` y `android/app/*.keystore`.

### Notas

- Esta versión consolida todo el trabajo introducido en los primeros tres commits del repositorio:
  - `f1c20f1` Initial commit: NetFlow - Network data monitoring app
  - `66ab173` chore: configurar release signing, ProGuard y applicationId
  - `a347c9f` docs: actualizar README con requisitos y permisos

[Keep a Changelog]: https://keepachangelog.com/en/1.1.0/
[Semantic Versioning]: https://semver.org/spec/v2.0.0.html
[2.1.0]: https://github.com/dony-aep/netflow/compare/v2.0.4...v2.1.0
[2.0.4]: https://github.com/dony-aep/netflow/compare/v2.0.3...v2.0.4
[2.0.3]: https://github.com/dony-aep/netflow/compare/v2.0.2...v2.0.3
[2.0.2]: https://github.com/dony-aep/netflow/compare/v2.0.1...v2.0.2
[2.0.1]: https://github.com/dony-aep/netflow/compare/v2.0.0...v2.0.1
[2.0.0]: https://github.com/dony-aep/netflow/compare/v1.0.1...v2.0.0
[1.0.1]: https://github.com/dony-aep/netflow/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/dony-aep/netflow/commits/v1.0.0
