@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.donyaep.netflow.ui.screens.updates

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.donyaep.netflow.core.update.GitHubUpdateService
import com.donyaep.netflow.core.update.UpdateCheckStatus

@Composable
fun UpdateScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    viewModel: UpdateViewModel = viewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    val state = uiState.value

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // ── Versión instalada ─────────────────────────────────────────────
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = cs.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = cs.primaryContainer,
                        ) {
                            Icon(
                                Icons.Rounded.SystemUpdateAlt,
                                contentDescription = null,
                                tint = cs.onPrimaryContainer,
                                modifier = Modifier.padding(12.dp).size(26.dp),
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "Versión instalada",
                                style = MaterialTheme.typography.labelMedium,
                                color = cs.onSurfaceVariant,
                            )
                            Text(
                                "v${state.currentVersion}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = cs.primary,
                            )
                        }
                    }
                }
            }

            // ── Estado del chequeo ────────────────────────────────────────────
            item {
                AnimatedContent(
                    targetState = Triple(state.isChecking, state.status, state.message),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "update_status",
                ) { (checking, status, message) ->
                    val (icon, tint, bg, text) = when {
                        checking -> Quad(null, cs.onSurfaceVariant, cs.surfaceContainerHigh, "Buscando actualizaciones…")
                        status == UpdateCheckStatus.UpToDate -> Quad(
                            Icons.Rounded.CheckCircle,
                            cs.onTertiaryContainer, cs.tertiaryContainer,
                            "La app está actualizada.",
                        )
                        status == UpdateCheckStatus.UpdateAvailable -> Quad(
                            Icons.Rounded.NewReleases,
                            cs.onSecondaryContainer, cs.secondaryContainer,
                            "Hay una versión más reciente disponible.",
                        )
                        status == UpdateCheckStatus.Error -> Quad(
                            Icons.Rounded.ErrorOutline,
                            cs.onErrorContainer, cs.errorContainer,
                            message ?: "No se pudo consultar el servidor.",
                        )
                        else -> Quad(
                            null, cs.onSurfaceVariant, cs.surfaceContainerHigh,
                            "Comprobando…",
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = bg,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            if (checking) {
                                CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
                            } else if (icon != null) {
                                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                            }
                            Text(
                                text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = tint,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }

            // ── Info de la release (solo si hay update disponible) ────────────
            state.releaseInfo?.takeIf { state.status == UpdateCheckStatus.UpdateAvailable }?.let { release ->
                item {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically(),
                        exit  = fadeOut() + shrinkVertically(),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = cs.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    "Nueva versión disponible",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                HorizontalDivider()
                                InfoRow("Versión", release.version)
                                if (release.publishedAtFormatted.isNotBlank()) {
                                    InfoRow("Publicada", release.publishedAtFormatted)
                                }
                                if (release.apkSizeFormatted.isNotBlank()) {
                                    InfoRow("Tamaño APK", release.apkSizeFormatted)
                                }
                                release.releaseNotes.takeIf { it.isNotBlank() }?.let { notes ->
                                    HorizontalDivider()
                                    Text(
                                        "Notas",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = cs.onSurfaceVariant,
                                    )
                                    Text(
                                        notes,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = cs.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Acción principal ──────────────────────────────────────────────
            item {
                val updateAvailable = state.status == UpdateCheckStatus.UpdateAvailable
                val alreadyChecked  = state.status != null && !state.isChecking
                if (updateAvailable) {
                    Button(
                        onClick = {
                            val url = state.releaseInfo?.apkDownloadUrl
                                ?: state.releaseInfo?.htmlUrl
                                ?: GitHubUpdateService.RELEASES_URL
                            openExternalUrl(context, url)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Descargar nueva versión")
                    }
                } else {
                    OutlinedButton(
                        onClick  = viewModel::checkForUpdates,
                        enabled  = !state.isChecking,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(if (alreadyChecked) "Revisar de nuevo" else "Buscar actualización")
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = cs.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Las actualizaciones se descargan directamente desde GitHub Releases. Al pulsar el botón de descarga, el navegador abrirá el archivo APK de la nueva versión.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun InfoRow(label: String, value: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun openExternalUrl(context: Context, url: String) {
    context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

