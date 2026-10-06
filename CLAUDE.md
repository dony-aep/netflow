# NetFlow

Monitor de tráfico de red para Android. Kotlin, Jetpack Compose y Material 3 Expressive.

## Comandos

- Compilar: `./gradlew :app:compileDebugKotlin`
- Lint: `./gradlew :app:lintDebug`. Debe terminar con 0 errores.
- Release: `./gradlew :app:assembleRelease`. Firma con `app/key.properties`; si falta, usa la clave de debug sin avisar.
- No hay pruebas propias, solo las de la plantilla. Un cambio se verifica compilando, pasando lint y probándolo en un dispositivo.

## Interfaz

- Antes de escribir o revisar Compose, cargar la skill global `android-m3-expressive`.
- `material3` sale de `compose-bom-alpha`, sin versión propia en el catálogo. Al subir el BOM, seguir la referencia de migración de esa skill y proponer los cambios antes de aplicarlos.
- Iconos: `Icons.*` solo para los que trae `material-icons-core`. El resto son vector drawables en `res/drawable/ic_<nombre>.xml`, usados con `ImageVector.vectorResource(R.drawable.ic_<nombre>)`. No añadir `material-icons-extended`.
- Los textos de la interfaz van en español, escritos en el propio composable. No hay `strings.xml` que mantener.

## Código

- `minSdk` es 26. Cualquier API posterior necesita una comprobación de `Build.VERSION.SDK_INT`; lint la exige con `NewApi`.
- `compileSdk` es 37.1 porque lo piden las alphas de Compose. `targetSdk` sigue en 36.
- Inyección de dependencias manual en `data/AppContainer.kt`. No añadir Hilt.
- Room exporta el esquema a `app/schemas/`. Un cambio de esquema sube `version`, añade su migración y commitea el JSON generado.
- En `NetFlowMonitorService`, el fallback a `WifiManager.connectionInfo` está deprecado a propósito: en muchos dispositivos es lo único que devuelve el SSID real. No quitarlo.
- El servicio de monitoreo es de tipo `specialUse`. No volver a `dataSync`: desde Android 15 ese tipo se corta a las 6 horas y no puede arrancarse desde `BOOT_COMPLETED`.

## Commits y changelog

- Conventional commits en español con ortografía correcta, tildes incluidas: `fix(monitoring): reiniciar contadores de notificación`. El historial antiguo las omite; no imitarlo.
- `CHANGELOG.md` sigue Keep a Changelog en español. Las entradas antiguas tampoco llevan tildes; las nuevas sí.
- Las skills se mantienen globales. No añadir copias al repo.

## Releases

Usar la skill global `/android-release`. Lo que esa skill no puede deducir:

- `versionCode` y `versionName` están en `app/build.gradle.kts` y se suben a mano.
- El tag debe ser `v` más el `versionName` exacto. La app compara ese tag con su versión instalada para avisar de actualizaciones.
- El APK se adjunta como `netflow_vX.Y.Z_release.apk`.

## Seguridad

- No commitear `app/key.properties` ni ningún `*.jks` o `*.keystore`.
