@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.donyaep.netflow.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.core.content.ContextCompat
import androidx.graphics.shapes.Morph
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.donyaep.netflow.R
import com.donyaep.netflow.core.monitoring.NetFlowMonitorServiceController
import com.donyaep.netflow.core.monitoring.NetworkType
import com.donyaep.netflow.ui.components.BUSY_DEGREES_PER_SECOND
import com.donyaep.netflow.ui.components.CALM_DEGREES_PER_SECOND
import com.donyaep.netflow.ui.components.NetworkSplit
import com.donyaep.netflow.ui.components.PulseControlHeight
import com.donyaep.netflow.ui.components.PulseLeadingShapes
import com.donyaep.netflow.ui.components.PulseTrailingShapes
import com.donyaep.netflow.ui.components.calmShape
import com.donyaep.netflow.ui.components.pulseShape
import com.donyaep.netflow.ui.components.rememberCookie12Morph
import com.donyaep.netflow.ui.components.rememberCookie9Morph
import com.donyaep.netflow.ui.components.rememberPulseRotation
import com.donyaep.netflow.ui.theme.AppCodeFontFamily
import kotlin.math.abs
import kotlin.math.ln

// ─────────────────────────────────────────────────────────────────────────────
// Route entry point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeRoute(
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingStart = remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && pendingStart.value) {
            NetFlowMonitorServiceController.start(context)
        } else if (!granted) {
            Toast.makeText(
                context,
                "Se necesita permiso de notificaciones para el monitoreo en primer plano.",
                Toast.LENGTH_LONG,
            ).show()
        }
        pendingStart.value = false
    }

    // Auto-inicio al entrar a la pantalla — igual que la versión Flutter
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NetFlowMonitorServiceController.start(context)
        } else {
            pendingStart.value = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    HomeScreen(
        state = uiState,
        modifier = modifier,
        onStartMonitoring = {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                NetFlowMonitorServiceController.start(context)
            } else {
                pendingStart.value = true
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onStopMonitoring = { NetFlowMonitorServiceController.stop(context) },
        onResetToday = {
            viewModel.resetTodayUsage()
            NetFlowMonitorServiceController.resetToday(context)
        },
        onOpenHistory = onOpenHistory,
        onOpenSettings = onOpenSettings,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    onResetToday: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val showConfirmReset = remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "NetFlow",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_history), contentDescription = "Historial")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Ajustes")
                    }
                },
                windowInsets = WindowInsets(0),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                PulseHero(state = state)
                NetworkStatusLine(state = state)
                TodayUsageSection(state = state)
                Spacer(Modifier.height(24.dp))
            }

            ServiceControlSection(
                state = state,
                onStartMonitoring = onStartMonitoring,
                onStopMonitoring = onStopMonitoring,
                onResetToday = { showConfirmReset.value = true },
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
            )
        }
    }

    if (showConfirmReset.value) {
        AlertDialog(
            onDismissRequest = { showConfirmReset.value = false },
            title = { Text("Reiniciar contadores del día") },
            text = { Text("Se borrará el registro de uso de datos de hoy. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmReset.value = false
                        onResetToday()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Reiniciar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmReset.value = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1 · Pulso
//
// La bajada vive en una forma grande que se festonea y gira más rápido cuanto
// más tráfico hay; la subida, en una pequeña que se le solapa. Sin conexión la
// grande pasa a una forma de cuatro lados en color de error y deja de girar.
// ─────────────────────────────────────────────────────────────────────────────

private const val CALM_BYTES_PER_SECOND = 2_000.0
private const val BUSY_BYTES_PER_SECOND = 2_000_000.0

// 0 con la red parada y 1 desde unos 2 MB/s. La escala es logarítmica para que
// navegar y ver vídeo se distingan aunque estén a órdenes de magnitud.
private fun activityLevel(bytesPerSecond: Long): Float {
    if (bytesPerSecond <= CALM_BYTES_PER_SECOND) return 0f
    val level = ln(bytesPerSecond / CALM_BYTES_PER_SECOND) / ln(BUSY_BYTES_PER_SECOND / CALM_BYTES_PER_SECOND)
    return level.toFloat().coerceAtMost(1f)
}

@Composable
private fun PulseHero(state: HomeUiState) {
    val cs = MaterialTheme.colorScheme
    val motionScheme = MaterialTheme.motionScheme
    // Con el servicio parado el tipo de red también llega como None; eso es pausa, no un corte.
    val offline = state.isMonitoring && state.networkType == NetworkType.None
    val isLive = state.isMonitoring && !offline

    // Un solo eje para la forma grande: de 0 a 1 es la actividad de bajada y de
    // 0 a -1 la pérdida de conexión. Así el cambio siempre pasa por el círculo.
    val downloadAxis by animateFloatAsState(
        targetValue = when {
            offline -> -1f
            isLive -> activityLevel(state.downloadBytesPerSecond)
            else -> 0f
        },
        animationSpec = motionScheme.slowSpatialSpec(),
        label = "downloadAxis",
    )
    val uploadLevel by animateFloatAsState(
        targetValue = if (isLive) activityLevel(state.uploadBytesPerSecond) else 0f,
        animationSpec = motionScheme.slowSpatialSpec(),
        label = "uploadLevel",
    )

    val downloadMorph = rememberCookie12Morph()
    val offlineMorph = remember { Morph(calmShape(points = 12), MaterialShapes.Cookie4Sided) }
    val uploadMorph = rememberCookie9Morph()

    val downloadLevel = downloadAxis.coerceIn(0f, 1f)
    val offlineLevel = (-downloadAxis).coerceIn(0f, 1f)
    val bigContainer = lerp(
        lerp(cs.surfaceContainerHigh, cs.primaryContainer, downloadLevel),
        cs.errorContainer,
        offlineLevel,
    )
    val bigContent = lerp(
        lerp(cs.onSurface, cs.onPrimaryContainer, downloadLevel),
        cs.onErrorContainer,
        offlineLevel,
    )
    val smallContainer = lerp(cs.surfaceContainerHighest, cs.tertiaryContainer, uploadLevel.coerceIn(0f, 1f))
    val smallContent = lerp(cs.onSurface, cs.onTertiaryContainer, uploadLevel.coerceIn(0f, 1f))


    val rotation = rememberPulseRotation(
        if (offline) 0f else lerp(CALM_DEGREES_PER_SECOND, BUSY_DEGREES_PER_SECOND, downloadLevel),
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        val big = (maxWidth * 0.82f).coerceAtMost(340.dp)
        val small = big * 0.475f
        val start = (maxWidth - big * 0.6625f - small) / 2

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(big * 0.744f + small),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = start)
                    .size(big),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { rotationZ = rotation.floatValue }
                        .pulseShape(
                            morph = if (downloadAxis >= 0f) downloadMorph else offlineMorph,
                            level = { abs(downloadAxis) },
                            color = { bigContainer },
                        ),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (isLive) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_downward),
                                contentDescription = null,
                                tint = bigContent,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Text(
                            text = when {
                                offline -> "Sin conexión"
                                isLive -> "Bajando"
                                else -> "En pausa"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = bigContent,
                        )
                    }
                    AnimatedContent(
                        targetState = state.downloadSpeedValue,
                        transitionSpec = {
                            (slideInVertically(animationSpec = motionScheme.fastEffectsSpec()) { -it / 2 } +
                                fadeIn(animationSpec = motionScheme.fastEffectsSpec())) togetherWith
                                (slideOutVertically(animationSpec = motionScheme.fastEffectsSpec()) { it / 2 } +
                                    fadeOut(animationSpec = motionScheme.fastEffectsSpec()))
                        },
                        label = "dlSpeed",
                    ) { value ->
                        val fontSize = when {
                            value.length <= 3 -> 96.sp
                            value.length == 4 -> 80.sp
                            value.length == 5 -> 64.sp
                            else -> 54.sp
                        }
                        Text(
                            text = value,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = AppCodeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = fontSize,
                                lineHeight = fontSize * 1.08f,
                                letterSpacing = (-3).sp,
                            ),
                            color = bigContent,
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = state.downloadSpeedUnit,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = bigContent,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .offset(x = start + big * 0.6625f, y = big * 0.744f)
                    .size(small),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pulseShape(
                            morph = uploadMorph,
                            level = { uploadLevel },
                            color = { smallContainer },
                            gapColor = cs.background,
                        ),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_upward),
                            contentDescription = null,
                            tint = smallContent,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "Subiendo",
                            style = MaterialTheme.typography.labelMedium,
                            color = smallContent,
                        )
                    }
                    AnimatedContent(
                        targetState = state.uploadSpeedValue,
                        transitionSpec = {
                            (slideInVertically(animationSpec = motionScheme.fastEffectsSpec()) { -it / 2 } +
                                fadeIn(animationSpec = motionScheme.fastEffectsSpec())) togetherWith
                                (slideOutVertically(animationSpec = motionScheme.fastEffectsSpec()) { it / 2 } +
                                    fadeOut(animationSpec = motionScheme.fastEffectsSpec()))
                        },
                        label = "ulSpeed",
                    ) { value ->
                        val fontSize = when {
                            value.length <= 4 -> 34.sp
                            value.length == 5 -> 28.sp
                            else -> 24.sp
                        }
                        Text(
                            text = value,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = AppCodeFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = fontSize,
                                lineHeight = fontSize * 1.18f,
                                letterSpacing = (-1).sp,
                            ),
                            color = smallContent,
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = state.uploadSpeedUnit,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = smallContent,
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkStatusLine(state: HomeUiState) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(
                when {
                    !state.isMonitoring -> R.drawable.ic_stop
                    state.networkType == NetworkType.Wifi -> R.drawable.ic_wifi
                    state.networkType == NetworkType.Mobile -> R.drawable.ic_network_cell
                    else -> R.drawable.ic_wifi_off
                },
            ),
            contentDescription = null,
            tint = cs.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = if (state.isMonitoring) state.connectionLabel else "Monitoreo en pausa",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2 · Hoy llevas
//
// El total del día, su reparto entre WiFi y datos móviles en una barra de dos
// tramos y, si hay límite configurado, el avance hacia él.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TodayUsageSection(state: HomeUiState) {
    val cs = MaterialTheme.colorScheme
    val hasLimit = state.dataLimitEnabled && state.dataLimitBytes > 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "Hoy llevas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Text(
                text = state.todayTotalLabel,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = AppCodeFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 44.sp,
                    lineHeight = 48.sp,
                    letterSpacing = (-1.5).sp,
                ),
                color = cs.onSurface,
            )
        }


        NetworkSplit(
            wifiShare = if (state.todayTotalBytes > 0L) {
                state.todayWifiBytes.toFloat() / state.todayTotalBytes
            } else {
                null
            },
            wifiLabel = state.todayWifiLabel,
            mobileLabel = state.todayMobileLabel,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(
                text = "${state.todayDownloadLabel} bajados",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
            )
            Text(
                text = "${state.todayUploadLabel} subidos",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
            )
        }

        if (hasLimit) {
            val progress = (state.cycleMobileBytes.toFloat() / state.dataLimitBytes.toFloat()).coerceIn(0f, 1f)
            val nearLimit = progress > 0.80f
            Spacer(Modifier.height(4.dp))
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (nearLimit) cs.error else cs.primary,
                trackColor = if (nearLimit) cs.errorContainer else cs.surfaceContainerHighest,
            )
            Text(
                text = "Datos móviles del ciclo: ${state.cycleMobileLabel} de ${state.dataLimitSummary}",
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3 · Control del servicio
//
// Dos botones conectados a todo el ancho: iniciar o detener el monitoreo y
// reiniciar los contadores del día.
// ─────────────────────────────────────────────────────────────────────────────


@Composable
private fun ServiceControlSection(
    state: HomeUiState,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    onResetToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motionScheme = MaterialTheme.motionScheme
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (state.isMonitoring) onStopMonitoring() else onStartMonitoring()
            },
            shapes = PulseLeadingShapes,
            modifier = Modifier
                .weight(1f)
                .height(PulseControlHeight),
        ) {
            // Animación de icono + etiqueta al cambiar el estado de monitoreo
            AnimatedContent(
                targetState = state.isMonitoring,
                transitionSpec = {
                    (slideInVertically(animationSpec = motionScheme.fastEffectsSpec()) { -it / 3 } +
                        fadeIn(animationSpec = motionScheme.fastEffectsSpec())) togetherWith
                        (slideOutVertically(animationSpec = motionScheme.fastEffectsSpec()) { it / 3 } +
                            fadeOut(animationSpec = motionScheme.fastEffectsSpec()))
                },
                label = "ctaContent",
            ) { monitoring ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = if (monitoring) ImageVector.vectorResource(R.drawable.ic_stop) else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = if (monitoring) "Detener monitoreo" else "Iniciar monitoreo",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp),
                    )
                }
            }
        }
        FilledTonalIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onResetToday()
            },
            shapes = PulseTrailingShapes,
            modifier = Modifier.size(PulseControlHeight),
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Reiniciar contadores del día",
                modifier = Modifier.size(26.dp),
            )
        }
    }
}
