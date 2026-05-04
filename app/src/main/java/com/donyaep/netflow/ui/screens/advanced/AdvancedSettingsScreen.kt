@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.donyaep.netflow.ui.screens.advanced

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

@Composable
fun AdvancedSettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var status by remember { mutableStateOf(readAdvancedStatus(context)) }
    var pendingBackgroundLaunch by remember { mutableStateOf(false) }

    // Actualiza el estado en tiempo real cada vez que la app vuelve al primer plano
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                status = readAdvancedStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Paso 2: ACCESS_BACKGROUND_LOCATION → lleva al usuario a los ajustes de ubicación de la app
    // para que seleccione "Permitir todo el tiempo".
    // `granted` = true solo si eligió esa opción.
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        status = readAdvancedStatus(context)
        val s = status
        scope.launch {
            val message = when {
                granted && s.locationEnabled ->
                    "Permisos de ubicación actualizados correctamente."
                granted && !s.locationEnabled ->
                    "Permiso concedido. Activa la ubicación del sistema para que se muestre el SSID."
                s.hasLocationPermission ->
                    // Volvió de ajustes pero eligió "Solo mientras se usa la app" u otra opción inferior
                    "Para SSID estable en segundo plano, selecciona \"Permitir todo el tiempo\" en los ajustes de ubicación."
                else ->
                    "Permiso denegado. Puedes habilitarlo en los ajustes del sistema."
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    // Paso 1: ACCESS_FINE_LOCATION → diálogo estándar del sistema.
    // Si se concede y API >= Q, lanza el paso 2 automáticamente.
    val fineLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        status = readAdvancedStatus(context)
        when {
            granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !status.hasBackgroundLocationPermission -> {
                pendingBackgroundLaunch = true
            }
            granted -> {
                // API < Q (background implícito) o ya tiene segundo plano → resultado final
                val s = status
                scope.launch {
                    val message = if (s.locationEnabled) {
                        "Permisos de ubicación actualizados correctamente."
                    } else {
                        "Permiso concedido. Activa la ubicación del sistema para que se muestre el SSID."
                    }
                    snackbarHostState.showSnackbar(message)
                }
            }
            else -> {
                scope.launch {
                    snackbarHostState.showSnackbar("Permiso denegado. Puedes habilitarlo en los ajustes del sistema.")
                }
            }
        }
    }

    // Encadena el paso 2 desde el scope de composición (no desde dentro del callback de otro launcher)
    LaunchedEffect(pendingBackgroundLaunch) {
        if (pendingBackgroundLaunch) {
            pendingBackgroundLaunch = false
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text("Ajustes avanzados") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
                windowInsets = WindowInsets(0),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            item {
                val locationFullyGranted = status.hasLocationPermission &&
                        status.hasBackgroundLocationPermission &&
                        status.locationEnabled
                AdvancedCard(
                    icon        = Icons.Rounded.LocationOn,
                    title       = "Ubicación para SSID",
                    description = "Permite mostrar el nombre de la red WiFi (SSID) en la notificación, incluso en segundo plano.",
                    rows        = listOf(
                        "Permiso en uso"           to if (status.hasLocationPermission) "Activo" else "Inactivo",
                        "Permiso en segundo plano" to if (status.hasBackgroundLocationPermission) "Activo" else "Inactivo",
                        "Servicio del sistema"     to if (status.locationEnabled) "Activo" else "Inactivo",
                    ),
                    isGranted    = locationFullyGranted,
                    onAction     = if (!locationFullyGranted) {
                        {
                            if (!status.hasLocationPermission) {
                                fineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            } else if (!status.hasBackgroundLocationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            }
                        }
                    } else null,
                    actionLabel  = "Configurar permisos de ubicación",
                )
            }

            item {
                AdvancedCard(
                    icon        = Icons.Rounded.BatteryChargingFull,
                    title       = "Optimización de batería",
                    description = "Ayuda a que el servicio de monitoreo permanezca estable cuando la app entra en segundo plano.",
                    rows        = listOf(
                        "Exención" to if (status.ignoresBatteryOptimizations) "Activa" else "Pendiente",
                    ),
                    isGranted    = status.ignoresBatteryOptimizations,
                    onAction     = if (!status.ignoresBatteryOptimizations) {
                        {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    } else null,
                    actionLabel  = "Deshabilitar optimización",
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ── Components ────────────────────────────────────────────────────────────────

@Composable
private fun StatusChip(value: String) {
    val cs = MaterialTheme.colorScheme
    val isActive = value == "Activo" || value == "Activa"
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = if (isActive) cs.tertiaryContainer else cs.errorContainer,
    ) {
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isActive) cs.onTertiaryContainer else cs.onErrorContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun AdvancedCard(
    icon: ImageVector,
    title: String,
    description: String,
    rows: List<Pair<String, String>>,
    isGranted: Boolean,
    onAction: (() -> Unit)?,
    actionLabel: String,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = cs.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Cabecera
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isGranted) cs.tertiaryContainer else cs.primaryContainer,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (isGranted) cs.onTertiaryContainer else cs.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp),
                    )
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (isGranted) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = "Concedido",
                        tint = cs.tertiary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
            )
            // Contenido animado: estado concedido vs. filas + botón
            AnimatedContent(
                targetState = isGranted,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "card_content_$title",
            ) { granted ->
                if (granted) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = cs.tertiaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = cs.tertiary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                "Todos los permisos concedidos",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = cs.onTertiaryContainer,
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HorizontalDivider()
                        rows.forEach { (label, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = cs.onSurfaceVariant,
                                )
                                StatusChip(value)
                            }
                        }
                        if (onAction != null) {
                            HorizontalDivider()
                            Button(
                                onClick = onAction,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(actionLabel)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private data class AdvancedStatus(
    val hasLocationPermission: Boolean,
    val hasBackgroundLocationPermission: Boolean,
    val locationEnabled: Boolean,
    val ignoresBatteryOptimizations: Boolean,
)

private fun readAdvancedStatus(context: Context): AdvancedStatus {
    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val hasBackgroundLocationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        hasLocationPermission
    }
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val powerManager    = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return AdvancedStatus(
        hasLocationPermission             = hasLocationPermission,
        hasBackgroundLocationPermission   = hasBackgroundLocationPermission,
        locationEnabled                   = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            true
        },
        ignoresBatteryOptimizations       = powerManager.isIgnoringBatteryOptimizations(context.packageName),
    )
}
