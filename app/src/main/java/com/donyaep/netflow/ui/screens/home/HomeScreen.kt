@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.donyaep.netflow.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.NetworkCell
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import com.donyaep.netflow.ui.theme.AppCodeFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.donyaep.netflow.core.monitoring.NetFlowMonitorServiceController
import com.donyaep.netflow.core.monitoring.NetworkType

// ─────────────────────────────────────────────────────────────────────────────
// Route entry point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeRoute(
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
) {
    val showConfirmReset = remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = 100.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { LiveSpeedHero(state = state) }
            item { TodayUsageCard(state = state) }
            item { Spacer(Modifier.height(8.dp)) }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            ServiceControlSection(
                state = state,
                onStartMonitoring = onStartMonitoring,
                onStopMonitoring = onStopMonitoring,
                onResetToday = { showConfirmReset.value = true },
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
// 1 · Live Speed Hero
//
// Sección hero centrada con tipografía a gran escala. Diseño minimalista sin
// card: las cifras son el protagonista. CircularWavyProgressIndicator como
// beacon de estado en vivo.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LiveSpeedHero(state: HomeUiState) {
    val cs = MaterialTheme.colorScheme
    val isLive = state.isMonitoring && state.networkType != NetworkType.None

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Network status pill ─────────────────────────────────────────────
        val motionScheme = MaterialTheme.motionScheme

        val targetPrimary: Color
        val targetContainer: Color
        val networkIcon: ImageVector
        val networkName: String
        when (state.networkType) {
            NetworkType.Wifi -> {
                targetPrimary = cs.primary
                targetContainer = cs.primaryContainer
                networkIcon = Icons.Rounded.Wifi
                networkName = "WiFi"
            }
            NetworkType.Mobile -> {
                targetPrimary = cs.tertiary
                targetContainer = cs.tertiaryContainer
                networkIcon = Icons.Rounded.NetworkCell
                networkName = "Datos Móviles"
            }
            NetworkType.None -> {
                targetPrimary = cs.error
                targetContainer = cs.errorContainer
                networkIcon = Icons.Rounded.WifiOff
                networkName = "Sin Conexión"
            }
        }

        val primaryColor by animateColorAsState(
            targetValue = targetPrimary,
            animationSpec = motionScheme.slowEffectsSpec(),
            label = "netPrimary",
        )
        val containerColor by animateColorAsState(
            targetValue = targetContainer,
            animationSpec = motionScheme.slowEffectsSpec(),
            label = "netContainer",
        )

        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = containerColor,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = networkIcon,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = networkName,
                    style = MaterialTheme.typography.titleSmall,
                    color = primaryColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (isLive) "Monitoreando red" else state.connectionLabel,
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
        )
        if (isLive) {
            Spacer(Modifier.height(6.dp))
            LoadingIndicator(modifier = Modifier.height(14.dp))
        }

        Spacer(Modifier.height(36.dp))

        // ── Bajada | Subida — dos columnas ───────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Bajada
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowDownward,
                        contentDescription = null,
                        tint = cs.error,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Bajada",
                        style = MaterialTheme.typography.labelLarge,
                        color = cs.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
                AnimatedContent(
                    targetState = state.downloadSpeedValue,
                    transitionSpec = {
                        (slideInVertically(animationSpec = motionScheme.fastEffectsSpec()) { -it / 2 } +
                            fadeIn(animationSpec = motionScheme.fastEffectsSpec())) togetherWith
                            (slideOutVertically(animationSpec = motionScheme.fastEffectsSpec()) { it / 2 } +
                                fadeOut(animationSpec = motionScheme.fastEffectsSpec()))
                    },
                    label = "dlSpeed",
                ) { v ->
                    Text(
                        text = v,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = AppCodeFontFamily,
                            fontWeight = FontWeight.Black,
                        ),
                        color = cs.onSurface,
                    )
                }
                Text(
                    text = state.downloadSpeedUnit,
                    style = MaterialTheme.typography.titleSmall,
                    color = cs.error.copy(alpha = 0.7f),
                )
            }

            // Subida
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowUpward,
                        contentDescription = null,
                        tint = cs.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Subida",
                        style = MaterialTheme.typography.labelLarge,
                        color = cs.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
                AnimatedContent(
                    targetState = state.uploadSpeedValue,
                    transitionSpec = {
                        (slideInVertically(animationSpec = motionScheme.fastEffectsSpec()) { -it / 2 } +
                            fadeIn(animationSpec = motionScheme.fastEffectsSpec())) togetherWith
                            (slideOutVertically(animationSpec = motionScheme.fastEffectsSpec()) { it / 2 } +
                                fadeOut(animationSpec = motionScheme.fastEffectsSpec()))
                    },
                    label = "ulSpeed",
                ) { v ->
                    Text(
                        text = v,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = AppCodeFontFamily,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = cs.onSurface,
                    )
                }
                Text(
                    text = state.uploadSpeedUnit,
                    style = MaterialTheme.typography.titleSmall,
                    color = cs.primary.copy(alpha = 0.7f),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2 · Today's Usage
//
// Sección de uso diario centrada. Total como cifra hero, barra de progreso
// ondulada M3E y desglose en grid limpio sin bordes.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TodayUsageCard(state: HomeUiState) {
    val cs = MaterialTheme.colorScheme

    val progressValue: Float
    val progressColor: Color
    val progressTrack: Color
    val progressLabel: String

    if (state.dataLimitEnabled && state.dataLimitBytes > 0) {
        progressValue = (state.todayTotalBytes.toFloat() / state.dataLimitBytes.toFloat())
            .coerceIn(0f, 1f)
        val nearLimit = progressValue > 0.80f
        progressColor = if (nearLimit) cs.error else cs.primary
        progressTrack = if (nearLimit) cs.errorContainer else cs.surfaceContainerHighest
        progressLabel = "Límite: ${state.dataLimitSummary}"
    } else {
        progressValue = 0f
        progressColor = cs.primary
        progressTrack = cs.surfaceContainerHighest
        progressLabel = "Sin límite"
    }

    val hasLimit = state.dataLimitEnabled && state.dataLimitBytes > 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Cabecera con fondo ────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = cs.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Uso de hoy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface,
                    )
                    if (hasLimit) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(cs.outlineVariant, CircleShape),
                        )
                        Text(
                            text = progressLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                if (hasLimit) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = state.todayTotalLabel,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = AppCodeFontFamily,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = cs.onSurface,
                        )
                        CircularWavyProgressIndicator(
                            progress = { progressValue },
                            modifier = Modifier.size(64.dp),
                            color = progressColor,
                            trackColor = progressTrack,
                        )
                    }
                } else {
                    Text(
                        text = state.todayTotalLabel,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = AppCodeFontFamily,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = cs.onSurface,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Desglose bajada / subida ─────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            UsageDetailItem(
                icon = Icons.Rounded.ArrowDownward,
                label = "Bajada",
                value = state.todayDownloadLabel,
                accentColor = cs.error,
                modifier = Modifier.weight(1f),
            )
            UsageDetailItem(
                icon = Icons.Rounded.ArrowUpward,
                label = "Subida",
                value = state.todayUploadLabel,
                accentColor = cs.primary,
                modifier = Modifier.weight(1f),
            )
        }

        // ── División WiFi / Móvil ────────────────────────────────────────────
        if (state.todayTotalBytes > 0L) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                UsageDetailItem(
                    icon = Icons.Rounded.Wifi,
                    label = "WiFi",
                    value = state.todayWifiLabel,
                    accentColor = cs.secondary,
                    modifier = Modifier.weight(1f),
                )
                UsageDetailItem(
                    icon = Icons.Rounded.NetworkCell,
                    label = "Móvil",
                    value = state.todayMobileLabel,
                    accentColor = cs.tertiary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun UsageDetailItem(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = AppCodeFontFamily),
                color = accentColor,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4 · Service Control Section
//
// Uses the M3E SplitButtonLayout — the leading half handles Start/Stop with an
// AnimatedContent transition; the trailing half gives one-tap access to Reset.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ServiceControlSection(
    state: HomeUiState,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    onResetToday: () -> Unit,
) {
    val motionScheme = MaterialTheme.motionScheme
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.LeadingButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (state.isMonitoring) onStopMonitoring() else onStartMonitoring()
                    },
                    modifier = Modifier.defaultMinSize(
                        minHeight = SplitButtonDefaults.MediumContainerHeight,
                    ),
                    contentPadding = SplitButtonDefaults.MediumLeadingButtonContentPadding,
                    shapes = SplitButtonDefaults.leadingButtonShapesFor(
                        SplitButtonDefaults.MediumContainerHeight,
                    ),
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
                            horizontalArrangement = Arrangement.spacedBy(ButtonDefaults.IconSpacing),
                        ) {
                            Icon(
                                imageVector = if (monitoring) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                            Text(
                                text = if (monitoring) "Detener monitoreo" else "Iniciar monitoreo",
                            )
                        }
                    }
                }
            },
            trailingButton = {
                // Trailing = reinicio de contadores del día (sin dropdown)
                SplitButtonDefaults.TrailingButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onResetToday()
                    },
                    modifier = Modifier.defaultMinSize(
                        minHeight = SplitButtonDefaults.MediumContainerHeight,
                    ),
                    contentPadding = SplitButtonDefaults.MediumTrailingButtonContentPadding,
                    shapes = SplitButtonDefaults.trailingButtonShapesFor(
                        SplitButtonDefaults.MediumContainerHeight,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Reiniciar contadores del día",
                        modifier = Modifier.size(SplitButtonDefaults.MediumTrailingButtonIconSize),
                    )
                }
            },
        )
    }
}
