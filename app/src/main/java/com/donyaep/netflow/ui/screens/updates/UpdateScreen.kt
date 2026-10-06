@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.donyaep.netflow.ui.screens.updates

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.donyaep.netflow.R
import com.donyaep.netflow.core.update.GitHubUpdateService
import com.donyaep.netflow.core.update.ReleaseInfo
import com.donyaep.netflow.core.update.UpdateCheckStatus
import com.donyaep.netflow.ui.components.BUSY_DEGREES_PER_SECOND
import com.donyaep.netflow.ui.components.CALM_DEGREES_PER_SECOND
import com.donyaep.netflow.ui.components.PulseControlHeight
import com.donyaep.netflow.ui.components.PulseLeadingShapes
import com.donyaep.netflow.ui.components.PulseSingleShapes
import com.donyaep.netflow.ui.components.PulseTrailingShapes
import com.donyaep.netflow.ui.components.pulseShape
import com.donyaep.netflow.ui.components.rememberCookie12Morph
import com.donyaep.netflow.ui.components.rememberCookie9Morph
import com.donyaep.netflow.ui.components.rememberPulseRotation
import com.donyaep.netflow.ui.theme.AppCodeFontFamily

@Composable
fun UpdateScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    viewModel: UpdateViewModel = viewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    val release = state.releaseInfo.takeIf { !state.isChecking && state.status == UpdateCheckStatus.UpdateAvailable }
    val failed = !state.isChecking && state.status == UpdateCheckStatus.Error

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text("Actualizaciones") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(rememberScrollState()),
            ) {
                UpdateHero(state = state, release = release, failed = failed)

                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (failed) {
                        Text(
                            text = state.message ?: "No se pudo consultar el servidor.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = cs.error,
                        )
                    }
                    Text(
                        text = "NetFlow se publica en GitHub. Al descargar se abre la página de la versión " +
                            "en tu navegador, y desde ahí bajas el APK.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = cs.onSurfaceVariant,
                    )
                    release?.let { ReleaseDetails(release = it) }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (release != null) {
                    Button(
                        onClick = { openExternalUrl(context, GitHubUpdateService.RELEASES_URL) },
                        shapes = PulseLeadingShapes,
                        modifier = Modifier
                            .weight(1f)
                            .height(PulseControlHeight),
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_download),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Descargar v${release.version}",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp),
                        )
                    }
                    FilledTonalIconButton(
                        onClick = viewModel::checkForUpdates,
                        shapes = PulseTrailingShapes,
                        modifier = Modifier.size(PulseControlHeight),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Buscar de nuevo",
                            modifier = Modifier.size(26.dp),
                        )
                    }
                } else {
                    FilledTonalButton(
                        onClick = viewModel::checkForUpdates,
                        enabled = !state.isChecking,
                        shapes = PulseSingleShapes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(PulseControlHeight),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (state.isChecking) "Buscando…" else "Buscar de nuevo",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp),
                        )
                    }
                }
            }
        }
    }
}

// ── Hero ──────────────────────────────────────────────────────────────────────
// La versión va dentro de la forma: lisa cuando la app está al día, festoneada
// y girando deprisa cuando hay una versión nueva, que desplaza la instalada a
// la forma pequeña.
@Composable
private fun UpdateHero(state: UpdateUiState, release: ReleaseInfo?, failed: Boolean) {
    val cs = MaterialTheme.colorScheme
    val motionScheme = MaterialTheme.motionScheme
    val checking = state.isChecking || state.status == null

    val level by animateFloatAsState(
        targetValue = when {
            release != null -> 1f
            checking -> 0.45f
            else -> 0f
        },
        animationSpec = motionScheme.slowSpatialSpec(),
        label = "updateLevel",
    )
    val container by animateColorAsState(
        targetValue = when {
            release != null -> cs.primaryContainer
            failed -> cs.errorContainer
            else -> cs.surfaceContainerHigh
        },
        animationSpec = motionScheme.slowEffectsSpec(),
        label = "updateContainer",
    )
    val content by animateColorAsState(
        targetValue = when {
            release != null -> cs.onPrimaryContainer
            failed -> cs.onErrorContainer
            else -> cs.onSurface
        },
        animationSpec = motionScheme.slowEffectsSpec(),
        label = "updateContent",
    )
    val rotation = rememberPulseRotation(
        when {
            failed -> 0f
            release != null || checking -> BUSY_DEGREES_PER_SECOND
            else -> CALM_DEGREES_PER_SECOND
        },
    )

    val label: String
    val version: String
    val note: String
    when {
        checking -> {
            label = "Buscando…"
            version = "v${state.currentVersion}"
            note = "Consultando GitHub"
        }
        release != null -> {
            label = "Versión nueva"
            version = "v${release.version}"
            note = "Lista para descargar"
        }
        failed -> {
            label = "No se pudo comprobar"
            version = "v${state.currentVersion}"
            note = "Es la que tienes instalada"
        }
        else -> {
            label = "Estás al día"
            version = "v${state.currentVersion}"
            note = "Es la última publicada"
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        val big = (maxWidth * 0.78f).coerceAtMost(320.dp)
        val small = big * 0.44f
        val start = (maxWidth - big * 0.727f - small) / 2

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(big * 0.773f + small),
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
                        .pulseShape(morph = rememberCookie12Morph(), level = { level }, color = { container }),
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(label, style = MaterialTheme.typography.titleMedium, color = content)
                    val fontSize = when {
                        version.length <= 6 -> 60.sp
                        version.length == 7 -> 52.sp
                        else -> 44.sp
                    }
                    Text(
                        text = version,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = AppCodeFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = fontSize,
                            lineHeight = fontSize * 1.14f,
                            letterSpacing = (-2.5).sp,
                        ),
                        color = content,
                        maxLines = 1,
                    )
                    Text(note, style = MaterialTheme.typography.bodyMedium, color = content)
                }
            }

            AnimatedVisibility(
                visible = release != null,
                enter = scaleIn(animationSpec = motionScheme.defaultSpatialSpec()) +
                    fadeIn(animationSpec = motionScheme.defaultEffectsSpec()),
                exit = scaleOut(animationSpec = motionScheme.fastSpatialSpec()) +
                    fadeOut(animationSpec = motionScheme.fastEffectsSpec()),
                modifier = Modifier.offset(x = start + big * 0.727f, y = big * 0.773f),
            ) {
                Box(
                    modifier = Modifier
                        .size(small)
                        .pulseShape(
                            morph = rememberCookie9Morph(),
                            level = { 0.4f },
                            color = { cs.surfaceContainerHighest },
                            gapColor = cs.background,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Tienes la", style = MaterialTheme.typography.labelMedium, color = cs.onSurface)
                        Text(
                            text = "v${state.currentVersion}",
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = AppCodeFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseDetails(release: ReleaseInfo) {
    val cs = MaterialTheme.colorScheme
    val facts = listOfNotNull(
        release.publishedAtFormatted.takeIf { it.isNotBlank() }?.let { "Publicada el $it" },
        release.apkSizeFormatted.takeIf { it.isNotBlank() },
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Qué trae la v${release.version}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (facts.isNotEmpty()) {
            Text(
                text = facts.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
            )
        }
        if (release.releaseNotes.isNotBlank()) {
            Text(
                text = release.releaseNotes,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
            )
        }
    }
}

private fun openExternalUrl(context: Context, url: String) {
    context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
